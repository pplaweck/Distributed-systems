import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;

public class StationMonitorActor extends AbstractBehavior<StationMonitorActor.MonitorStationCommand> {
    public ActorRef<ActorDispatcher.DispatcherCommand> dispatcher;
    public int queryId;
    public String monitorStationName;
    private final Map<Integer, Long> timeTable = new HashMap<>();
    public StationMonitorActor(ActorContext<MonitorStationCommand> context) {
        super(context);
    }
    public interface MonitorStationCommand {

    }


    public static final class MonitorStationCommandSetUp implements MonitorStationCommand{
        public final String monitorStationName;
        public final ActorRef<ActorDispatcher.DispatcherCommand> dispatcher;
        public MonitorStationCommandSetUp(String monitorStationName, ActorRef<ActorDispatcher.DispatcherCommand> dispatcher) {
            this.monitorStationName = monitorStationName;
            this.dispatcher = dispatcher;
        }
    }

    public static final class MonitorStationCommandGetStatus implements MonitorStationCommand {
        public final int queryId;
        public final int firstSatId;
        public final int range;
        public final int timeout;
        public final ActorRef<MonitorStationCommand> replyTo;

        public MonitorStationCommandGetStatus(int queryId, int firstSatId, int range, int timeout, ActorRef<MonitorStationCommand> replyTo) {
            this.queryId = queryId;
            this.firstSatId = firstSatId;
            this.range = range;
            this.timeout = timeout;
            this.replyTo = replyTo;
        }
    }

    public static final class MonitorStationCommandOnResponse implements MonitorStationCommand {
        public final int queryId;
        public final Map<Integer, SatelliteAPI.Status> errors;
        public final double percentOnTime;


        public MonitorStationCommandOnResponse(int queryId, Map<Integer, SatelliteAPI.Status> errors, double percentOnTime) {
            this.queryId = queryId;
            this.errors = errors;
            this.percentOnTime = percentOnTime;
        }
    }

    public static Behavior<MonitorStationCommand> create() {
        return Behaviors.setup(StationMonitorActor::new);
    }

    // --- define message handlers
    @Override
    public Receive<MonitorStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(MonitorStationCommandGetStatus.class, this::onMonitorStationCommandGetStatus)
                .onMessage(MonitorStationCommandSetUp.class, this::monitorStationCommandSetUp)
                .onMessage(MonitorStationCommandOnResponse.class, this::monitorStationCommandOnResponse)
                .build();
    }

    private Behavior<MonitorStationCommand> monitorStationCommandSetUp(MonitorStationCommandSetUp monitorStationCommandSetUp) {
        System.out.println("Got command - setUp()");
        this.monitorStationName = monitorStationCommandSetUp.monitorStationName;
        this.dispatcher = monitorStationCommandSetUp.dispatcher;
        System.out.println("setUp() - done");
        return this;
    }

    private Behavior<MonitorStationCommand> onMonitorStationCommandGetStatus(MonitorStationCommandGetStatus monitorStationCommandGetStatus) {
        dispatcher.tell(new ActorDispatcher.DispatcherCommandGetStatus(
                this.queryId,
                monitorStationCommandGetStatus.firstSatId,
                monitorStationCommandGetStatus.range,
                monitorStationCommandGetStatus.timeout,
                getContext().getSelf()));
                timeTable.put(queryId, System.currentTimeMillis());
        System.out.println("Got command - getStatus()");
        System.out.println("getStatus() - done, sending to dispatcher");
        return this;
    }

    private Behavior<MonitorStationCommand> monitorStationCommandOnResponse(MonitorStationCommandOnResponse monitorStationCommandOnResponse) {
        long time = System.currentTimeMillis() - timeTable.get(monitorStationCommandOnResponse.queryId);
        if(monitorStationCommandOnResponse.errors.size() != 0) {
            System.out.println("Monitor station name: " + this.monitorStationName);
            System.out.println("On time percentage: " + monitorStationCommandOnResponse.percentOnTime + "%");
            System.out.println("Number of errors: " + monitorStationCommandOnResponse.errors.size());
            System.out.println("Response time: " + time + " ms.");
            monitorStationCommandOnResponse.errors.forEach((sat, value) -> System.out.println("Satellite " + sat + " got error named: " + value + "."));
            System.out.println();
            System.out.println();
        }
        else {
            System.out.println("Monitor station name: " + this.monitorStationName + " has no errors!");
        }
        return this;
    }

}
