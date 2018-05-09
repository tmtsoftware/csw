package csw.services.event.perf

import akka.actor.ActorSystem
import org.HdrHistogram.Histogram
import EventUtils._

class ResultReporter(name: String, actorSystem: ActorSystem) {

  val reporter = BenchmarkFileReporter(name, actorSystem, logSettings = false)

  def printResult(
      id: Int,
      testSettings: TestSettings,
      histogram: Histogram,
      totalReceived: Long,
      totalTime: Double,
      outOfOrderCount: Long
  ): Unit = {
    import testSettings._

    val throughput = totalReceived / nanosToSeconds(totalTime)

    val totalDropped = totalTestMsgs - totalReceived

    def percentile(p: Double) = histogram.getValueAtPercentile(p) / 1000.0

    reporter.reportResults(
      s"================= $testName Results [Subscriber-$id] =================\n" +
      "Throughput: \n" +
      f"          Throughput:   $throughput%,.0f msg/s \n" +
      f"          Payload:      ${throughput * payloadSize}%,.0f bytes/s \n" +
      f"          Total size:   ${throughput * totalSize}%,.0f bytes/s \n" +
      f"          Events recd:  $totalReceived \n" +
      f"          Time taken:   ${totalTime / Math.pow(10, 9)}%,.0f seconds \n" +
      s"          Dropped:      $totalDropped \n" +
      s"          Out of order: $outOfOrderCount \n" +
      "Latency: \n" +
      f"          50%%ile: ${percentile(50.0)}%.0f µs \n" +
      f"          90%%ile: ${percentile(90.0)}%.0f µs \n" +
      f"          99%%ile: ${percentile(99.0)}%.0f µs \n"
    )
  }

  def printModelObsResult(
      subSetting: SubSetting,
      subId: Int,
      histogram: Histogram,
      totalReceived: Long,
      totalTime: Double,
      outOfOrderCount: Long
  ): Unit = {

    import subSetting._

    val throughput = totalReceived / nanosToSeconds(totalTime)

    val totalDropped = totalTestMsgs - totalReceived

    def percentile(p: Double) = histogram.getValueAtPercentile(p) / 1000.0

    reporter.reportResults(
      s"================= Results [$name][Subscriber-$subId] =================\n" +
      "Throughput: \n" +
      f"          Throughput:   $throughput%,.0f msg/s \n" +
      f"          Payload:      ${throughput * payloadSize}%,.0f bytes/s \n" +
      f"          Events recd:  $totalReceived \n" +
      f"          Time taken:   ${totalTime / Math.pow(10, 9)}%,.0f seconds \n" +
      s"          Dropped:      $totalDropped \n" +
      s"          Out of order: $outOfOrderCount \n" +
      "Latency: \n" +
      f"          50%%ile: ${percentile(50.0)}%.0f µs \n" +
      f"          90%%ile: ${percentile(90.0)}%.0f µs \n" +
      f"          99%%ile: ${percentile(99.0)}%.0f µs \n"
    )
  }
}
