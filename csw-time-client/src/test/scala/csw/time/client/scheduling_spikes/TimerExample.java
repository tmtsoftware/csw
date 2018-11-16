package csw.time.client.scheduling_spikes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Schedule a task that executes once every second.
 */
class TimerExample {
    public static void main(String[] args) {
    Timer timer= new Timer();
        ArrayList<Long> buffer  = new ArrayList<>();

    System.out.println("About to schedule task.");
    timer.schedule(new MyTask(buffer), 0, 100);
    System.out.println("Task scheduled.");
  }
}

class MyTask extends TimerTask {

    private ArrayList<Long> buf;

    MyTask(ArrayList<Long> buf) {
        this.buf = buf;
    }

  private int numWarningBeeps = 1000;

    @Override
    public void run() {

    if (numWarningBeeps > 0) {
      Instant instant = Instant.now();
      buf.add(instant.toEpochMilli());
      numWarningBeeps -= 1;
    } else {
      System.out.println(buf);
//    timer.cancel(); //Not necessary because we call System.exit
      System.exit(0); //Stops the AWT thread (and everything else)
    }
  }
}
