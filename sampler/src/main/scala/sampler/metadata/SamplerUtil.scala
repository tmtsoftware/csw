package sampler.metadata

import org.HdrHistogram.Histogram

import scala.collection.mutable.ListBuffer

object SamplerUtil {

  val histogramEventsDiff: Histogram = new Histogram(3)
  val histogramSanpshot: Histogram   = new Histogram(3)

  def recordHistogram(eventTimeDiff: Long, snapshotTime: Long): Unit = {
    histogramEventsDiff.recordValue(eventTimeDiff)
    histogramSanpshot.recordValue(snapshotTime)
  }

  def printAggregates(eventTimeDiffList: ListBuffer[Long], snapshotTimeList: ListBuffer[Long]): Unit = {
    val eventTimeDiffAvg: Double = eventTimeDiffList.sum.toDouble / eventTimeDiffList.size
    val snapshotTimeAvg: Double  = snapshotTimeList.sum.toDouble / snapshotTimeList.size

    println(
      s"================= Percentiles EventsDiff =================\n" +
        "Latency: \n" +
        f"          50%%ile : ${histogramEventsDiff.getValueAtPercentile(50.0) / 1.0}%.0f millis \n" +
        f"          90%%ile : ${histogramEventsDiff.getValueAtPercentile(90.0) / 1.0}%.0f millis \n" +
        f"          99%%ile : ${histogramEventsDiff.getValueAtPercentile(99.0) / 1.0}%.0f millis \n"
    )
    println(s"Average of event time diff: $eventTimeDiffAvg millis")
    println(s"Standard deviation of event Time Diff: ${standardDeviation(eventTimeDiffList, eventTimeDiffAvg)} millis")
    println("\n=================================\n")
    println(
      s"================= Percentiles Snapshots =================\n" +
        "Latency: \n" +
        f"          50%%ile : ${histogramSanpshot.getValueAtPercentile(50.0) / 1.0}%.0f millis \n" +
        f"          90%%ile : ${histogramSanpshot.getValueAtPercentile(90.0) / 1.0}%.0f millis \n" +
        f"          99%%ile : ${histogramSanpshot.getValueAtPercentile(99.0) / 1.0}%.0f millis \n"
    )
    println(s"Average of snapshot time: $snapshotTimeAvg millis")
    println(s"Standard deviation of snapshot time: ${standardDeviation(snapshotTimeList, snapshotTimeAvg)} millis")

  }

  private def standardDeviation(diffList: ListBuffer[Long], avg: Double): Double = {
    val variance = diffList.map(_.toDouble).map(a => math.pow(a - avg, 2)).sum / diffList.size
    math.sqrt(variance)
  }

}
