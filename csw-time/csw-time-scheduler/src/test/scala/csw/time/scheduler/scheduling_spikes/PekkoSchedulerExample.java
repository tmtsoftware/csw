/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.scheduling_spikes;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.time.clock.natives.TimeLibrary;
import csw.time.clock.natives.models.TimeSpec;

import java.time.Duration;
import java.util.ArrayList;

public class PekkoSchedulerExample {

    public static void main(String[] args) {
        Config config = ConfigFactory.parseString("pekko.scheduler.tick-duration=1ms").withFallback(ConfigFactory.load());
        ActorSystem system = ActorSystem.create(Behaviors.empty(), "PekkoSchedulerExample", config);

        system.scheduler().scheduleAtFixedRate(Duration.ofMillis(0), Duration.ofMillis(1), new MyTask1(), system.executionContext());
    }

}

class MyTask1 implements Runnable {

    private final ArrayList<String> buf = new ArrayList<>();
    private int numWarningBeeps = 901000;

    @Override
    public void run() {
        if (numWarningBeeps > 0) {
            TimeSpec timeSpec = new TimeSpec();

            TimeLibrary.clock_gettime(0, timeSpec);
            long s = timeSpec.seconds.longValue();
            String n = String.format("%09d", timeSpec.nanoseconds.longValue());
            if (numWarningBeeps <= 1000) buf.add(s + "" + n);
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
