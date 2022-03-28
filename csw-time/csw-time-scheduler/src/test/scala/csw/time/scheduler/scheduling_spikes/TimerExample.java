/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.scheduling_spikes;

import csw.time.clock.natives.TimeLibrary;
import csw.time.clock.natives.models.TimeSpec;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Schedule a task that executes once every second.
 */
class TimerExample {
    public static void main(String[] args) {
        Timer timer = new Timer();
        System.out.println("About to schedule task.");
        timer.schedule(new MyTask(), 0, 1);
        System.out.println("Task scheduled.");
    }
}

class MyTask extends TimerTask {

    private final LinkedList<String> buf = new LinkedList<String>();

    private int numWarningBeeps = 901000;

    @Override
    public void run() {

        if (numWarningBeeps > 0) {
//      Instant instant = Instant.now();
//      buf.add(instant.toEpochMilli());

            TimeSpec timeSpec = new TimeSpec();

            TimeLibrary.clock_gettime(0, timeSpec);
            long s = timeSpec.seconds.longValue();
            String n = String.format("%09d", timeSpec.nanoseconds.longValue());
            if (numWarningBeeps <= 1000) buf.add(s + "" + n);

            numWarningBeeps -= 1;
        } else {
            buf.forEach(System.out::println);
//    timer.cancel(); //Not necessary because we call System.exit
            System.exit(0); //Stops the AWT thread (and everything else)
        }
    }
}
