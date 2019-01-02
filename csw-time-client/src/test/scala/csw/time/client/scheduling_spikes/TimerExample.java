package csw.time.client.scheduling_spikes;

import csw.clock.natives.TimeLibrary;
import csw.clock.natives.models.TimeSpec;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Schedule a task that executes once every second.
 */
class TimerExample {
    public static void main(String[] args) {
    Timer timer= new Timer();
    System.out.println("About to schedule task.");
    timer.schedule(new MyTask(), 0, 100);
    System.out.println("Task scheduled.");
  }
}

class MyTask extends TimerTask {

    private LinkedList<String> buf = new LinkedList<String>();

    private int numWarningBeeps = 1000;

    @Override
    public void run() {

    if (numWarningBeeps > 0) {
//      Instant instant = Instant.now();
//      buf.add(instant.toEpochMilli());


        TimeSpec timeSpec = new TimeSpec();

        TimeLibrary.clock_gettime(0, timeSpec);
        long s = timeSpec.seconds.longValue();
        String n = String.format("%09d",timeSpec.nanoseconds.longValue());
        buf.add(s+""+n);


        numWarningBeeps -= 1;
    } else {
      System.out.println(buf);
//    timer.cancel(); //Not necessary because we call System.exit
      System.exit(0); //Stops the AWT thread (and everything else)
    }
  }
}
