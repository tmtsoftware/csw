package csw.services.event.perf.commons

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.perf.reporter.{ResultReporter, TestRateReporter}
import csw.services.event.perf.utils.EventUtils._
import csw.services.event.perf.wiring.{TestConfigs, TestWiring}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import org.HdrHistogram.Histogram

import scala.concurrent.Future

class PerfSubscriber(
    prefix: Prefix,
    pubId: Int,
    subId: Int,
    eventsSetting: EventsSetting,
    reporter: TestRateReporter,
    sharedSubscriber: EventSubscriber,
    testConfigs: TestConfigs,
    testWiring: TestWiring
) {

  import eventsSetting._
  import testConfigs._
  import testWiring.wiring._

  private val subscriber: EventSubscriber =
    if (testConfigs.shareConnection) sharedSubscriber else testWiring.subscriber

  val histogram: Histogram   = new Histogram(SECONDS.toNanos(10), 3)
  private val resultReporter = new ResultReporter(prefix.prefix, actorSystem)

  var startTime         = 0L
  var totalTime         = 0L
  var aggregatedLatency = 0L
  var eventsReceived    = 0L
  var lastId            = 0
  var outOfOrderCount   = 0
  var lastCurrentId     = 0

  private val testEventName = EventName(s"$testEventS-$pubId")
  private val endEventName  = EventName(s"$endEventS-$pubId")

  private val eventKeys = Set(EventKey(prefix, testEventName), EventKey(prefix, endEventName))

  private val eventsToDrop = warmup + eventKeys.size //inclusive of latest events from subscription

  def subscription: Source[Event, EventSubscription] =
    if (isPatternSubscriber) {
      val pattern = if (redisEnabled) redisPattern else kafkaPattern
      subscriber.pSubscribe(prefix.subsystem, pattern)
    } else subscriber.subscribe(eventKeys)

  def startSubscription(): Future[Done] =
    subscription
      .drop(eventsToDrop)
      .takeWhile {
        case SystemEvent(_, _, `endEventName`, _, _) ⇒ false
        case e @ _ ⇒
          if (isPatternSubscriber & e.eventKey.key.contains(endEventS)) false else true
      }
      .watchTermination()(Keep.right)
      .runForeach(report)

  private def report(event: Event): Unit = {
    val currentTime          = getNanos(Instant.now()).toLong
    val eventOriginationTime = getNanos(event.eventTime.time).toLong
    val latency              = currentTime - eventOriginationTime
    aggregatedLatency += latency

    if (eventsReceived == 0) startTime = currentTime.toLong

    eventsReceived += 1
    totalTime = currentTime - startTime

    reporter.onMessage(1, payloadSize)

    try {
      histogram.recordValue(latency)
    } catch {
      case _: ArrayIndexOutOfBoundsException ⇒
    }

    val currentId = event.eventId.id.toInt
    val inOrder   = currentId >= lastId
    lastId = currentId

    if (!inOrder) outOfOrderCount += 1
  }

  def totalDropped(): Long = totalTestMsgs - eventsReceived
  def avgLatency(): Long   = aggregatedLatency / eventsReceived

  def printResult(): Unit =
    resultReporter.printResult(
      subId,
      totalDropped(),
      payloadSize,
      histogram,
      eventsReceived,
      totalTime,
      outOfOrderCount,
      avgLatency()
    )

  def isPatternSubscriber: Boolean = patternBasedSubscription & prefix.prefix.contains("pattern")

}
