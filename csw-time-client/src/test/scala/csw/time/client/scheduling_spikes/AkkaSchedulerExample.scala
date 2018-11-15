package csw.time.client.scheduling_spikes

import java.time.Instant

import akka.actor.ActorSystem

import scala.concurrent.duration.DurationInt

object AkkaSchedulerExample {

  val callable: Runnable = () => {
    def foo(): Unit = {
      val instant = Instant.now
      System.out.println("Beep! -> " + instant + " :" + instant.toEpochMilli)
    }

    foo()
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("some")
    import system.dispatcher

    system.scheduler.schedule(0.millis, 1.millis, callable)
  }
}
