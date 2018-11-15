package csw.time.client.scheduling_spikes

import java.awt.Toolkit
import java.time.Instant
import java.util.{Timer, TimerTask}

/**
 * Schedule a task that executes once every second.
 */
object AnnoyingBeep {
  def main(args: Array[String]): Unit = {
    System.out.println("About to schedule task.")
    new AnnoyingBeep()
    System.out.println("Task scheduled.")
  }
}

class AnnoyingBeep() {
  var toolkit: Toolkit = _
  var timer: Timer     = _

  toolkit = Toolkit.getDefaultToolkit
  timer = new Timer
  timer.schedule(new RemindTask(),
                 0, //initial delay
                 1) //subsequent rate

  class RemindTask extends TimerTask {
    var numWarningBeeps = 200

    override def run(): Unit = {
      if (numWarningBeeps > 0) {
//        toolkit.beep()
        val instant = Instant.now()
        System.out.println("Beep! -> " + instant + " :" + instant.toEpochMilli)
        numWarningBeeps -= 1
      } else {
        toolkit.beep()
        System.out.println("Time's up!")
        //timer.cancel(); //Not necessary because we call System.exit
        System.exit(0) //Stops the AWT thread (and everything else)

      }
    }
  }

}
