package csw.event.client.perf.reporter

import akka.actor.typed.ActorSystem
import csw.event.client.perf.utils.EventUtils._
import org.HdrHistogram.Histogram

class ResultReporter(test: String, actorSystem: ActorSystem[_]) {

  val reporter = BenchmarkFileReporter(test, actorSystem, logSettings = false)

  def printResult(
      id: Int,
      totalDropped: Long,
      payloadSize: Int,
      histogram: Histogram,
      totalReceived: Long,
      totalTime: Double,
      outOfOrderCount: Long,
      avgLatency: Long
  ): Unit = {

    val throughput = totalReceived / nanosToSeconds(totalTime)

    def percentile(p: Double) = histogram.getValueAtPercentile(p) / 1000.0

    reporter.reportResults(
      s"================= Results [$test] [Subscriber-$id] =================\n" +
        "Throughput: \n" +
        f"          Throughput:   $throughput%,.0f msg/s \n" +
        f"          Payload:      ${throughput * payloadSize}%,.0f bytes/s \n" +
        f"          Events recd:  $totalReceived \n" +
        f"          Time taken:   ${totalTime / Math.pow(10, 9)}%,.0f seconds \n" +
        s"          Dropped:      $totalDropped \n" +
        s"          Out of order: $outOfOrderCount \n" +
        "Latency: \n" +
        f"          50%%ile : ${percentile(50.0)}%.0f µs \n" +
        f"          90%%ile : ${percentile(90.0)}%.0f µs \n" +
        f"          99%%ile : ${percentile(99.0)}%.0f µs \n" +
        f"          Average: ${nanosToMicros(avgLatency.toDouble)}%.0f µs \n"
    )
  }

}
