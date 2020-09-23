package sampler.metadata

import scala.collection.mutable.ListBuffer

object SamplerUtil {

  def printAggregates(eventTimeDiffList: ListBuffer[Long], snapshotTimeList: ListBuffer[Long]): Unit = {
    val eventTimeDiffAvg: Double = eventTimeDiffList.sum.toDouble / eventTimeDiffList.size
    val snapshotTimeAvg: Double  = snapshotTimeList.sum.toDouble / snapshotTimeList.size

    println("Unit in millis")
    println(s"Average of event time diff $eventTimeDiffAvg")
    println(s"Average of snapshot time $snapshotTimeAvg")
    println(s"Standard deviation of event Time Diff: ${standardDeviation(eventTimeDiffList, eventTimeDiffAvg)}")
    println(s"Standard deviation of snapshot time: ${standardDeviation(snapshotTimeList, snapshotTimeAvg)}")
  }

  private def standardDeviation(diffList: ListBuffer[Long], avg: Double): Double = {
    val variance = diffList.map(_.toDouble).map(a => math.pow(a - avg, 2)).sum / diffList.size
    math.sqrt(variance)
  }

}
