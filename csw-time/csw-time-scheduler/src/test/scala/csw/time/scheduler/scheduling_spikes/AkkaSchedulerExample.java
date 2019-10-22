package csw.time.scheduler.scheduling_spikes;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.time.clock.natives.TimeLibrary;
import csw.time.clock.natives.models.TimeSpec;

import java.time.Duration;
import java.util.ArrayList;

public class AkkaSchedulerExample {

    public static void main(String[] args) {
        Config config = ConfigFactory.parseString("akka.scheduler.tick-duration=1ms").withFallback(ConfigFactory.load());
        ActorSystem system = ActorSystem.create(Behaviors.empty(), "AkkaSchedulerExample", config);

        system.scheduler().scheduleAtFixedRate(Duration.ofMillis(0), Duration.ofMillis(1), new MyTask1(), system.executionContext());
    }

}

class MyTask1 implements Runnable {

    private ArrayList<String> buf = new ArrayList<>();
    private int numWarningBeeps = 1000;

    @Override
    public void run() {
        if (numWarningBeeps > 0) {
            TimeSpec timeSpec = new TimeSpec();

            TimeLibrary.clock_gettime(0, timeSpec);
            long s = timeSpec.seconds.longValue();
            String n = String.format("%09d", timeSpec.nanoseconds.longValue());
            buf.add(s + "" + n);
            numWarningBeeps -= 1;
        } else {
            for (int i = 0; i < 1000; i++) {
                System.out.println(buf.get(i));
            }

            //    timer.cancel(); //Not necessary because we call System.exit
            System.exit(0); //Stops the AWT thread (and everything else)
        }
    }
}
