import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.util.Random;

public class Main {
    public static Behavior<Void> create() {
        return Behaviors.setup(
                context -> {
                    Thread.sleep(1000);
                    return Behaviors.receive(Void.class)
                            .build();
                });
    }

    public static void main(String[] args) {
        File configFile = new File("src/main/dispatcher.conf");
        Config config = ConfigFactory.parseFile(configFile);
        System.out.println("Dispatcher config: " + config);
        ActorSystem<ActorDispatcher.DispatcherCommand> dispatcher = ActorSystem.create(ActorDispatcher.create(), "dispatcher");
        ActorSystem<StationMonitorActor.MonitorStationCommand> station1 = ActorSystem.create(StationMonitorActor.create(), "Akka_Factory");
        ActorSystem<StationMonitorActor.MonitorStationCommand> station2 = ActorSystem.create(StationMonitorActor.create(), "Akka_Spaceship");
        ActorSystem<StationMonitorActor.MonitorStationCommand> station3 = ActorSystem.create(StationMonitorActor.create(), "Akka_Pluton_Is_A_Planet");
        station1.tell(new StationMonitorActor.MonitorStationCommandSetUp("Akka_Factory", dispatcher));
        station2.tell(new StationMonitorActor.MonitorStationCommandSetUp("Akka_Spaceship", dispatcher));
        station3.tell(new StationMonitorActor.MonitorStationCommandSetUp("Akka_Pluton_Is_A_Planet", dispatcher));

        Random rand = new Random();
        int first_sat_id;
        int range = 50;
        int timeout = 300;

        first_sat_id  = 100;
        station1.tell(new StationMonitorActor.MonitorStationCommandGetStatus(1, first_sat_id + rand.nextInt(50), range, timeout, station1));
        station1.tell(new StationMonitorActor.MonitorStationCommandGetStatus(2, first_sat_id+rand.nextInt(50), range, timeout, station1));
        station2.tell(new StationMonitorActor.MonitorStationCommandGetStatus(3, first_sat_id+rand.nextInt(50), range, timeout, station2));
        station2.tell(new StationMonitorActor.MonitorStationCommandGetStatus(4, first_sat_id+rand.nextInt(50), range, timeout, station2));
        station3.tell(new StationMonitorActor.MonitorStationCommandGetStatus(5, first_sat_id+rand.nextInt(50), range, timeout, station3));
        station3.tell(new StationMonitorActor.MonitorStationCommandGetStatus(6, first_sat_id+rand.nextInt(50), range, timeout, station3));
    }
}

