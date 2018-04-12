package csw.services.event.perf

import akka.actor.ActorSystem
import org.HdrHistogram.Histogram
import EventUtils._

class ResultReporter(name: String, actorSystem: ActorSystem) {

  val reporter = BenchmarkFileReporter(name, actorSystem)

  def printResult(
      id: Int,
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

    def percentile(p: Double) = histogram.getValueAtPercentile(p) / 1000.0

    reporter.reportResults(
      s"================= $testName Results [Subscriber-$id] =================\n" +
      "Throughput: \n" +
      f"          throughput: $throughput%,.0f msg/s \n" +
      f"          payload: ${throughput * payloadSize}%,.0f bytes/s \n" +
      f"          total size:${throughput * totalSize}%,.0f bytes/s \n" +
      s"          total dropped $totalDropped \n" +
      s"          total out of order $outOfOrderCount \n" +
      s"          payload size $payloadSize \n" +
      s"          total size (payload + metadata) $totalSize \n" +
      s"          ${totalTime / Math.pow(10, 6)} ms to deliver $totalReceived messages \n" +
      "Latency: \n" +
      f"          50%%ile: ${percentile(50.0)}%.0f µs \n" +
      f"          90%%ile: ${percentile(90.0)}%.0f µs \n" +
      f"          99%%ile: ${percentile(99.0)}%.0f µs \n"
    )
  }
}
