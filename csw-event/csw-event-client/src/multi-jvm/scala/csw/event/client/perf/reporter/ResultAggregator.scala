package csw.event.client.perf.reporter

import java.io.PrintStream
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.{ByteString, Timeout}
import csw.event.api.scaladsl.{EventSubscriber, EventSubscription}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.perf.utils.EventUtils
import csw.event.client.perf.utils.EventUtils._
import csw.params.events.{Event, SystemEvent}
import org.HdrHistogram.Histogram
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime

class ResultAggregator(
    scenarioName: String,
    testName: String,
    subscriber: EventSubscriber,
    expPerfEventCount: Int,
    actorRef: ActorRef[AggregatedResult]
)(implicit val system: ActorSystem[_]) {

  private val eventHandler: Behavior[Event] = Behaviors.setup { _ =>
    val histogram               = new Histogram(SECONDS.toNanos(10), 3)
    val initialLatencyHistogram = new Histogram(SECONDS.toNanos(10), 3)
    var throughput              = 0d
    var newEvent                = false
    var receivedPerfEventCount  = 0
    var totalDropped            = 0L
    var outOfOrderCount         = 0L
    var avgLatency              = 0L

    def aggregateResult(): (LatencyPlots, ThroughputPlots, InitialLatencyPlots) = {

      def percentile(histogram: Histogram, p: Double): Double = nanosToMicros(histogram.getValueAtPercentile(p))

      val throughputPlots = ThroughputPlots(
        PlotResult().add(testName, throughput),
        PlotResult().add(testName, totalDropped),
        PlotResult().add(testName, outOfOrderCount)
      )

      val latencyPlots = LatencyPlots(
        PlotResult().add(testName, percentile(histogram, 50.0)),
        PlotResult().add(testName, percentile(histogram, 90.0)),
        PlotResult().add(testName, percentile(histogram, 99.0)),
        PlotResult().add(testName, nanosToMicros(avgLatency))
      )

      val initialLatencyPlots = InitialLatencyPlots(
        PlotResult().add(testName, percentile(initialLatencyHistogram, 50.0)),
        PlotResult().add(testName, percentile(initialLatencyHistogram, 90.0)),
        PlotResult().add(testName, percentile(initialLatencyHistogram, 99.0))
      )

      histogram.outputPercentileDistribution(
        new PrintStream(BenchmarkFileReporter(s"$scenarioName/Aggregated-$testName", system, logSettings = false).fos),
        1000.0
      )
      (latencyPlots, throughputPlots, initialLatencyPlots)
    }

    Behaviors.receiveMessage[Event] {
      case event: SystemEvent if newEvent =>
        receivedPerfEventCount += 1
        val histogramBuffer = ByteString(event.get(histogramKey).get.values).asByteBuffer
        histogram.add(Histogram.decodeFromByteBuffer(histogramBuffer, SECONDS.toNanos(10)))

        val initialLatencyHistogramBuffer =
          ByteString(event.get(initialLatencyHistogramKey).get.values).asByteBuffer
        initialLatencyHistogram.add(Histogram.decodeFromByteBuffer(initialLatencyHistogramBuffer, SECONDS.toNanos(10)))

        throughput += event.get(throughputKey).get.head
        outOfOrderCount += event.get(totalOutOfOrderKey).get.head
        totalDropped += event.get(totalDroppedKey).get.head
        val avgLatencyTmp = event.get(avgLatencyKey).get.head
        avgLatency = if (avgLatency == 0) avgLatencyTmp else (avgLatency + avgLatencyTmp) / 2

        if (receivedPerfEventCount == expPerfEventCount) {
          val (latencyPlots, throughputPlots, initialLatencyPlots) = aggregateResult()
          actorRef ! AggregatedResult(latencyPlots, throughputPlots, initialLatencyPlots)
        }
        Behaviors.same
      case _ => newEvent = true; Behaviors.same
    }
  }

  implicit private val timeout: Timeout          = Timeout(20.seconds)
  private val eventHandlerActor: ActorRef[Event] = system.systemActorOf(eventHandler, "result-agg").await

  def startSubscription(): EventSubscription = subscriber.subscribeActorRef(Set(EventUtils.perfEventKey), eventHandlerActor)
}

case class AggregatedResult(
    latencyPlots: LatencyPlots,
    throughputPlots: ThroughputPlots,
    initialLatencyPlots: InitialLatencyPlots
)
