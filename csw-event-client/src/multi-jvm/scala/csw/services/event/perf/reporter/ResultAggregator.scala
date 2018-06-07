package csw.services.event.perf.reporter

import java.io.PrintStream
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.util.ByteString
import csw.messages.events.{Event, SystemEvent}
import csw.services.event.perf.utils.EventUtils
import csw.services.event.perf.utils.EventUtils._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import org.HdrHistogram.Histogram

class ResultAggregator(
    scenarioName: String,
    testName: String,
    subscriber: EventSubscriber,
    expPerfEventCount: Int,
    actorRef: ActorRef[AggregatedResult]
)(implicit val system: ActorSystem) {

  private val histogram              = new Histogram(SECONDS.toNanos(10), 3)
  private var throughput             = 0d
  private var newEvent               = false
  private var receivedPerfEventCount = 0
  private var totalDropped           = 0L
  private var outOfOrderCount        = 0L
  private var avgLatency             = 0L

  def startSubscription(): EventSubscription = subscriber.subscribeCallback(Set(EventUtils.perfEventKey), onEvent)

  private def onEvent(event: Event): Unit = event match {
    case event: SystemEvent if newEvent ⇒
      receivedPerfEventCount += 1
      val histogramBuffer = ByteString(event.get(histogramKey).get.values).asByteBuffer
      histogram.add(Histogram.decodeFromByteBuffer(histogramBuffer, SECONDS.toNanos(10)))

      throughput += event.get(throughputKey).get.head
      outOfOrderCount += event.get(totalOutOfOrderKey).get.head
      totalDropped += event.get(totalDroppedKey).get.head
      val avgLatencyTmp = event.get(avgLatencyKey).get.head
      avgLatency = if (avgLatency == 0) avgLatencyTmp else (avgLatency + avgLatencyTmp) / 2

      if (receivedPerfEventCount == expPerfEventCount) {
        val (latencyPlots, throughputPlots) = aggregateResult()
        actorRef ! AggregatedResult(latencyPlots, throughputPlots)
      }
    case _ ⇒ newEvent = true
  }

  private def aggregateResult(): (LatencyPlots, ThroughputPlots) = {

    def percentile(p: Double): Double = nanosToMicros(histogram.getValueAtPercentile(p))

    val throughputPlots = ThroughputPlots(
      PlotResult().add(testName, throughput),
      PlotResult().add(testName, totalDropped),
      PlotResult().add(testName, outOfOrderCount)
    )

    val latencyPlots = LatencyPlots(
      PlotResult().add(testName, percentile(50.0)),
      PlotResult().add(testName, percentile(90.0)),
      PlotResult().add(testName, percentile(99.0)),
      PlotResult().add(testName, nanosToMicros(avgLatency))
    )

    histogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter(s"$scenarioName/Aggregated-$testName", system, logSettings = false).fos),
      1000.0
    )
    (latencyPlots, throughputPlots)
  }

}

case class AggregatedResult(latencyPlots: LatencyPlots, throughputPlots: ThroughputPlots)
