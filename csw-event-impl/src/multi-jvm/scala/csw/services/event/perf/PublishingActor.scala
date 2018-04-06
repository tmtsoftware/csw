package csw.services.event.perf

import java.io.PrintStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.{NANOSECONDS, SECONDS}

import akka.actor._
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Source}
import com.typesafe.config.Config
import csw.messages.events.EventName
import csw.services.event.perf.EventServicePerfSpec.Target
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.EventPublisher
import org.HdrHistogram.Histogram
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}

class PublishingActor(
    target: Target,
    targets: Array[Target],
    testSettings: TestSettings,
    throughputPlotRef: ActorRef,
    latencyPlotRef: ActorRef,
    reporter: BenchmarkFileReporter
) extends Actor
    with MockitoSugar {

  val numTargets: Int = targets.length

  import testSettings._
  val payload: Array[Byte] = ("0" * testSettings.payloadSize).getBytes("utf-8")
  var startTime            = 0L
  var remaining: Long      = totalMessages
  var maxRoundTripMillis   = 0L

  var flowControlId      = 0
  var pendingFlowControl = Map.empty[Int, Int]

  import Messages._

  private implicit val actorSystem: ActorSystem = context.system

  private val config: Config          = actorSystem.settings.config
  private val throttlingElements: Int = config.getInt("csw.test.EventThroughputSpec.throttling.elements")
  private val throttlingDuration: FiniteDuration = {
    val d = config.getDuration("csw.test.EventThroughputSpec.throttling.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }

  val publisher: EventPublisher = new TestWiring(actorSystem).publisher

  def receive: Receive = {
    case Init        ⇒ targets.foreach(_.tell(Init(target.ref), self))
    case Initialized ⇒ runWarmup()
  }

  def runWarmup(): Unit = {
    sendBatch(warmup = true) // first some warmup
    publisher.publish(event(startEvent))
    context.become(warmup)
  }

  def warmup: Receive = {
    case Start ⇒
      println("======================================")
      println(
        s"${self.path.name}: Starting benchmark of $totalMessages messages with " +
        s"${if (batching) s"burst size $burstSize"
        else s"throttling of ${throttlingElements}msgs/${throttlingDuration.toSeconds}s"} " +
        s"and payload size $payloadSize"
      )

      startTime = System.nanoTime
      remaining = totalMessages
      sent.indices.foreach(i ⇒ sent(i) = 0)

      context.become(active)
      val t0 = System.nanoTime()
      sendBatch(warmup = false)
      sendFlowControl(t0)
  }

  def active: Receive = {
    case c @ FlowControl(id, t0) ⇒
      val targetCount = pendingFlowControl(id)
      if (targetCount - 1 == 0) {
        pendingFlowControl -= id
        val now      = System.nanoTime()
        val duration = NANOSECONDS.toMillis(now - t0)
        maxRoundTripMillis = math.max(maxRoundTripMillis, duration)

        sendBatch(warmup = false)
        sendFlowControl(now)
      } else {
        // waiting for FlowControl from more targets
        pendingFlowControl = pendingFlowControl.updated(id, targetCount - 1)
      }
  }
  val sent = new Array[Long](targets.length)

  var noOfEndResultReceived   = 0
  var aggregatedTotalReceived = 0L
  var endResultMsgRecTime     = 0L
  var aggregatedHistogram     = new Histogram(SECONDS.toNanos(10), 3)

  def waitingForEndResult: Receive = {
    case EndResult(totalReceived, histogram, totalTime) ⇒
      if (!singlePublisher) {
        printThroughputResult(totalReceived, histogram, totalTime)
        context.stop(self)
      } else {
        noOfEndResultReceived += 1
        aggregatedTotalReceived += totalReceived
        endResultMsgRecTime = Math.max(endResultMsgRecTime, totalTime)
        aggregatedHistogram.add(histogram)
        if (noOfEndResultReceived == publisherSubscriberPairs) {
          printThroughputResult(aggregatedTotalReceived, aggregatedHistogram, endResultMsgRecTime)
          context.stop(self)
        }
      }
  }

  private def printThroughputResult(totalReceived: Long, histogram: Histogram, totalTime: Long): Unit = {
    val took = NANOSECONDS.toMillis(totalTime)
    val throughput =
      if (singlePublisher) totalReceived * 1000.0 / took else totalReceived * publisherSubscriberPairs * 1000.0 / took

    val totalDropped =
      if (singlePublisher) totalMessages * publisherSubscriberPairs - totalReceived
      else totalMessages - totalReceived

    println("======================================")
    reporter.reportResults(
      s"=== ${reporter.testName} ${self.path.name}: " +
      f"throughput $throughput%,.0f msg/s, " +
      f"${throughput * payloadSize}%,.0f bytes/s (payload), " +
      f"${throughput * totalSize(context.system)}%,.0f bytes/s (total" +
      s"dropped $totalDropped, " +
      s"burst size $burstSize, " +
      s"payload size $payloadSize, " +
      s"total size ${totalSize(context.system)}, " +
      s"$took ms to deliver $totalReceived messages"
    )

    printLatencyResults(histogram, totalTime)

    throughputPlotRef ! PlotResult().add(testName, throughput * payloadSize * testSettings.publisherSubscriberPairs / 1024 / 1024)
  }

  def printLatencyResults(histogram: Histogram, totalDurationNanos: Long): Unit = {
    def percentile(p: Double): Double = histogram.getValueAtPercentile(p) / 1000.0
    val throughput                    = 1000.0 * histogram.getTotalCount / math.max(1, totalDurationNanos.nanos.toMillis)

    reporter.reportResults(
      s"===== ${reporter.testName} $testName [${self.path.name}]: Latency Results ===== \n" +
      f"        50%%ile: ${percentile(50.0)}%.0f µs \n" +
      f"        90%%ile: ${percentile(90.0)}%.0f µs \n" +
      f"        99%%ile: ${percentile(99.0)}%.0f µs \n" +
      f"        rate  : $throughput%,.0f msg/s \n" +
      "=============================================================="
    )
    println(s"Histogram of latencies in microseconds (µs) [${self.path.name}].")

    histogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter.apply(s"${self.path.name}", actorSystem, logSettings = false).fos),
      1000.0
    )

    val latencyPlots = LatencyPlots(
      PlotResult().add(testName, percentile(50.0)),
      PlotResult().add(testName, percentile(90.0)),
      PlotResult().add(testName, percentile(99.0))
    )

    latencyPlotRef ! latencyPlots
  }

  def sendBatch(warmup: Boolean): Unit = {

    def makeEvent(counter: Int, id: Int) = {
      if (warmup) event(warmupEvent, payload = payload)
      else eventWithNanos(EventName(s"$testEvent.${id + 1}"), totalMessages - remaining + counter, payload)
    }

    val batchSize =
      if (warmup) 1000
      else if (batching) math.min(remaining, burstSize)
      else totalMessages

    def eventId(i: Int) = if (singlePublisher) 0 else i % numTargets

    if (batching) {
      var i = 0
      while (i < batchSize) {
        val id = eventId(i)
        sent(id) += 1

        Await.result(publisher.publish(makeEvent(i, id)), 5.seconds)
        i += 1
      }
      remaining -= batchSize
    } else {
      val source = Source(0 until batchSize.toInt)
        .throttle(throttlingElements, throttlingDuration, throttlingElements, ThrottleMode.Shaping)
        .map { counter ⇒
          val id = eventId(counter)
          sent(id) += 1
          makeEvent(counter, id)
        }
        .watchTermination()(Keep.right)

      Await.result(publisher.publish(source), 5.minutes)
      remaining -= totalMessages
    }
  }

  def sendFlowControl(t0: Long): Unit = {
    if (remaining <= 0) {
      context.become(waitingForEndResult)
      publisher.publish(event(endEvent))
    } else {
      flowControlId += 1
      pendingFlowControl = pendingFlowControl.updated(flowControlId, targets.length)
      Await.result(publisher.publish(flowCtlEvent(flowControlId, t0, self.path.name)), 5.seconds)
    }
  }
}

object PublishingActor {

  def props(
      mainTarget: Target,
      targets: Array[Target],
      testSettings: TestSettings,
      throughputPlotRef: ActorRef,
      latencyPlotRef: ActorRef,
      reporter: BenchmarkFileReporter
  ) =
    Props(new PublishingActor(mainTarget, targets, testSettings, throughputPlotRef, latencyPlotRef, reporter))

}
