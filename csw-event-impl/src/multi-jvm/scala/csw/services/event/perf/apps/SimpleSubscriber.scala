package csw.services.event.perf.apps

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.services.event.perf.EventUtils._
import csw.services.event.perf._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import org.HdrHistogram.Histogram

import scala.concurrent.{ExecutionContextExecutor, Future}

class SimpleSubscriber(testSettings: TestSettings, reporter: TestRateReporter, id: Int)(implicit val system: ActorSystem) {

  import testSettings._

  implicit val mat: ActorMaterializer       = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val subscriber: EventSubscriber = new TestWiring(system).subscriber
  private val histogram: Histogram        = new Histogram(SECONDS.toNanos(10), 3)
  private val resultReporter              = new ResultReporter(testName, system)

  private val warmupCount = 1000

  var startTime       = 0L
  var totalTime       = 0L
  var eventsReceived  = 0L
  var lastId          = 0
  var outOfOrderCount = 0
  var lastCurrentId   = 0
  val subscription: Source[Event, EventSubscription] =
    subscriber.subscribe(Set(EventKey(s"$testEventKey-$id"), EventKey(s"${prefix.prefix}.$endEventS-$id")))

  val endEvent = EventName(s"${EventUtils.endEventS}-$id")

  def startSubscription(): Future[Done] = {
    subscription
      .drop(warmupCount)
      .takeWhile {
        case SystemEvent(_, _, `endEvent`, _, _) ⇒ false
        case _                                   => true
      }
      .watchTermination()(Keep.right)
      .runForeach(report)
  }

  private def report(event: Event): Unit = {

    if (eventsReceived == 0)
      startTime = getNanosFromInstant(Instant.now()).toLong

    eventsReceived += 1
    val currentTime = getNanosFromInstant(Instant.now()).toLong
    totalTime = currentTime - startTime

    val latency = (getNanosFromInstant(Instant.now()) - getNanosFromInstant(event.eventTime.time)).toLong
    reporter.onMessage(1, payloadSize)

    try {
      histogram.recordValue(latency)
    } catch {
      case e: ArrayIndexOutOfBoundsException ⇒
    }

    val currentId = event.eventId.id.toInt
    val inOrder   = currentId >= lastId
    lastId = currentId

    if (!inOrder) {
      outOfOrderCount += 1
    }
  }

  def printResult(): Unit = {
    resultReporter.printThroughputResult(testSettings, histogram, eventsReceived, totalTime, outOfOrderCount)
    resultReporter.printLatencyResults(testSettings, histogram, totalTime)
  }

}
