import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ActorDispatcher extends AbstractBehavior<ActorDispatcher.DispatcherCommand> {

    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    public ActorDispatcher(ActorContext<DispatcherCommand> context) {
        super(context);
    }

    public interface DispatcherCommand {
    }

    public static class DispatcherCommandGetStatus implements DispatcherCommand {

        public final int queryId;
        public final int firstSatId;
        public final int range;
        public final int timeout;
        private final ActorRef<StationMonitorActor.MonitorStationCommand> replyToMonitorStation;

        public DispatcherCommandGetStatus(
                int queryId,
                int firstSatId,
                int range,
                int timeout,
                ActorRef<StationMonitorActor.MonitorStationCommand> replyToMonitorStation) {
            this.queryId = queryId;
            this.firstSatId = firstSatId;
            this.range = range;
            this.timeout = timeout;
            this.replyToMonitorStation = replyToMonitorStation;
        }
    }

    public static Behavior<ActorDispatcher.DispatcherCommand> create() {
        return Behaviors.setup(ActorDispatcher::new);
    }


    @Override
    public Receive<DispatcherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(DispatcherCommandGetStatus.class, this::onDispatcherCommandGetStatus)
                .build();
    }

    public Behavior<ActorDispatcher.DispatcherCommand> onDispatcherCommandGetStatus(DispatcherCommandGetStatus dispatcherCommandGetStatus) throws ExecutionException, InterruptedException {
        Map<Integer, SatelliteAPI.Status> errors = new HashMap<>();
        Map<Integer, Long> startTimes = new HashMap<>();
        Map<Integer, Future<SatelliteAPI.Status>> satelliteStatus = new HashMap<>();
        long time = 0;
        for (int satId = dispatcherCommandGetStatus.firstSatId; satId < dispatcherCommandGetStatus.firstSatId + dispatcherCommandGetStatus.range; satId++) {
            int finalSatId = satId;
            satelliteStatus.put(satId, executorService.submit(() -> SatelliteAPI.getStatus(finalSatId)));
            startTimes.put(satId, System.currentTimeMillis());
        }

        var done = 0;
        long startTime = System.currentTimeMillis();
        while (!satelliteStatus.isEmpty()) {

            for (int satId = dispatcherCommandGetStatus.firstSatId; satId < dispatcherCommandGetStatus.firstSatId + dispatcherCommandGetStatus.range; satId++) {
                time = System.currentTimeMillis() - startTimes.get(satId);
                if (satelliteStatus.containsKey(satId)) {
                    Future<SatelliteAPI.Status> satelliteStatFuture = satelliteStatus.get(satId);
                    if (satelliteStatFuture.isDone()) {
                        done++;
                        if (satelliteStatFuture.get() != SatelliteAPI.Status.OK) {
                            errors.put(satId, satelliteStatFuture.get());
                        }
                        satelliteStatus.remove(satId);
                    } else if (time > dispatcherCommandGetStatus.timeout) {
                        satelliteStatus.remove(satId);
                    }
                }
            }

        }

        float donePercentage = done * 100.f / dispatcherCommandGetStatus.range;
        StationMonitorActor.MonitorStationCommandOnResponse response = new StationMonitorActor.MonitorStationCommandOnResponse(dispatcherCommandGetStatus.queryId, errors, donePercentage );
        dispatcherCommandGetStatus.replyToMonitorStation.tell(response);

        return this;
    }
}
