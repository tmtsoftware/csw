package csw.services.event.perf.apps

import java.io.PrintStream

import akka.actor.ActorSystem
import csw.services.event.perf.{BenchmarkFileReporter, LatencyPlots, PlotResult, TestSettings}
import org.HdrHistogram.Histogram

class ResultReporter(name: String, actorSystem: ActorSystem) {

  val reporter = BenchmarkFileReporter(name, actorSystem)

  def printThroughputResult(
      testSettings: TestSettings,
      histogram: Histogram,
      totalReceived: Long,
      totalTime: Double,
      outOfOrderCount: Long
  ): Unit = {
    import testSettings._

    val throughput =
      if (singlePublisher) totalReceived / nanosToSeconds(totalTime)
      else totalReceived * publisherSubscriberPairs / nanosToSeconds(totalTime)

    val totalDropped =
      if (singlePublisher) totalMessages * publisherSubscriberPairs - totalReceived
      else totalMessages - totalReceived

    println("======================================")
    reporter.reportResults(
      s"=== ${reporter.testName} : " +
      f"throughput $throughput%,.0f msg/s, " +
      f"${throughput * payloadSize}%,.0f bytes/s (payload), " +
      f"${throughput * totalSize}%,.0f bytes/s, " +
      s"total dropped $totalDropped, " +
      s"total out of order $outOfOrderCount, " +
      s"burst size $burstSize, " +
      s"payload size $payloadSize, " +
      s"total size $totalSize, " +
      s"${totalTime / Math.pow(10, 6)} ms to deliver $totalReceived messages"
    )
  }

  def printLatencyResults(
      testSettings: TestSettings,
      histogram: Histogram,
      totalTime: Double,
  ): Unit = {

    import testSettings._
    def percentile(p: Double) = histogram.getValueAtPercentile(p) / 1000.0
    val throughput            = histogram.getTotalCount / math.max(1, nanosToSeconds(totalTime))

    reporter.reportResults(
      s"===== ${reporter.testName} $testName : Latency Results ===== \n" +
      f"        50%%ile: ${percentile(50.0)}%.0f µs \n" +
      f"        90%%ile: ${percentile(90.0)}%.0f µs \n" +
      f"        99%%ile: ${percentile(99.0)}%.0f µs \n" +
      f"        rate  : $throughput%,.0f msg/s \n" +
      "=============================================================="
    )
    println(s"Histogram of latencies in microseconds (µs).")

    histogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter.apply(s"PerfSpec", actorSystem, logSettings = false).fos),
      1000.0
    )

    val latencyPlots = LatencyPlots(
      PlotResult().add(testName, percentile(50.0)),
      PlotResult().add(testName, percentile(90.0)),
      PlotResult().add(testName, percentile(99.0))
    )

//      latencyPlotRef ! latencyPlots
  }

  def nanosToMillis(nanos: Double): Double  = nanos / Math.pow(10, 6)
  def nanosToSeconds(nanos: Double): Double = nanos / Math.pow(10, 9)

//    throughputPlotRef ! PlotResult().add(testName, throughput * payloadSize * testSettings.publisherSubscriberPairs / 1024 / 1024)

}
