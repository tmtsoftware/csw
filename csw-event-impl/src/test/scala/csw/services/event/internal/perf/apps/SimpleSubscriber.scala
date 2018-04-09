package csw.services.event.internal.perf.apps

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.ActorSystem
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import org.HdrHistogram.Histogram
import EventUtils._

import scala.collection.mutable

object SimpleSubscriber extends App {

  implicit val system: ActorSystem = ActorSystem("sub")
  private val wiring               = new TestWiring(system)
  val payload: Array[Byte]         = ("0" * 100).getBytes("utf-8")

  val noOfSubscribers = 2

  var histograms: List[Histogram] = List.fill[Histogram](noOfSubscribers)(new Histogram(SECONDS.toNanos(10), 3))

  var totalMsgsReceived: mutable.Buffer[Int] = mutable.Buffer.fill[Int](noOfSubscribers)(0)
  var record                                 = false

  for (n ← 0 until noOfSubscribers) {
    wiring.subscriber
      .subscribeCallback(eventKeys + EventKey(s"$testEventKey-$n") + EventKey(s"${prefix.prefix}.$endEventS-$n"), onEvent)
  }

  private def onEvent(event: Event): Unit = {
    event match {
      case SystemEvent(_, _, `startEvent`, _, _) ⇒ record = true
      case SystemEvent(_, _, end, _, _) if end.name.startsWith("end") && record && totalMsgsReceived(extractId(end)) > 10 ⇒ {
        val id = extractId(end)
        histograms(id).outputPercentileDistribution(System.out, 1000.0)

        println("==================================================")
        println(s"totalMsgsReceived = ${totalMsgsReceived(id)}")
        println("==================================================")
        system.terminate()
      }
      case Event.invalidEvent           ⇒
      case event: SystemEvent if record ⇒ report(event)
    }
  }

  private def getNanosFromInstant(instant: Instant): Long = instant.getEpochSecond * Math.pow(10, 9).toLong + instant.getNano

  private def extractId(end: EventName): Int = end.name.split("-").tail.head.toInt

  private def report(event: SystemEvent): Unit = {
    val id = extractId(event.eventName)
    totalMsgsReceived(id) += 1

    val nanos = getNanosFromInstant(Instant.now()) - getNanosFromInstant(event.eventTime.time)

    try {
      histograms(id).recordValue(nanos)
    } catch {
      case e: ArrayIndexOutOfBoundsException ⇒
    }
  }

}
