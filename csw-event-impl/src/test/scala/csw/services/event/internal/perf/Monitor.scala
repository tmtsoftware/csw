package csw.services.event.internal.perf

import akka.NotUsed
import akka.stream.scaladsl.Flow
import csw.messages.ccs.events.Event

import scala.collection.mutable

object Monitor {

  val cumulative: Flow[Event, Event, NotUsed] = flow(cumulative = true)
  val resetting: Flow[Event, Event, NotUsed]  = flow(cumulative = false)

  val step = 2000

  private def flow(cumulative: Boolean) = Flow[Event].statefulMapConcat { () =>
    val mode = if (cumulative) "Cumulative" else "Resetting"
    println(s"--------starting run in $mode----------")

    var lastId          = 0
    var printTime       = 0L
    var count           = 0
    var outOfOrderCount = 0
    var lastCurrentId   = 0

    var startTime = System.currentTimeMillis()
    var latencies = newMap()

    event =>
      val eventTime    = event.eventTime.time.toEpochMilli
      val currentTime  = System.currentTimeMillis
      val latency      = currentTime - eventTime
      val accTime      = currentTime - startTime
      val shouldPrint  = (accTime - printTime) > step
      val currentId    = event.eventId.id.toInt
      val isOutOfOrder = (currentId - lastId) < 1

      lastId = currentId

      count += 1

      if (isOutOfOrder) {
        outOfOrderCount += 1
      }

      latencies(latency) += 1

      def throughput(cnt: Int) = if (accTime != 0) ((cnt * 1000) / accTime).toString else "unknown"

      def latenciesStr = {
        val percentiles    = Latency.accumulate(latencies).map(_.percentile(count))
        val stdPercentiles = Percentile.selectStdDeviations(percentiles)
        stdPercentiles.map(p => s"${f"${p.percentile}%2.1f"} -> ${p.latency}ms").mkString("(", ", ", ")")
      }

      def droppedCount = (currentId - lastCurrentId) - count

      if (shouldPrint) {
        println(
          s"throughput:${throughput(count)}   latencies:$latenciesStr   out-of-order:$outOfOrderCount   dropThroughput:${throughput(droppedCount)}"
        )
        if (cumulative) {
          printTime = accTime
        } else {
          count = 0
          outOfOrderCount = 0
          lastCurrentId = currentId
          startTime = currentTime
          latencies = newMap()
        }
      }

      List(event)
  }

  private def newMap() = mutable.Map.empty[Long, Long].withDefaultValue(0)

}

case class Latency(latency: Long, count: Long) {
  def +(other: Latency): Latency          = Latency(other.latency, count + other.count)
  def percentile(total: Long): Percentile = Percentile(latency, count * 100.0 / total)
}

object Latency {
  def accumulate(latencies: mutable.Map[Long, Long]): List[Latency] =
    latencies
      .map { case (lat, cnt) => Latency(lat, cnt) }
      .toList
      .sortBy(_.latency)
      .scanLeft(Latency(0, 0))(_ + _)
}

case class Percentile(latency: Long, percentile: Double)

object Percentile {
  private val standardDeviations = List(95.5, 99.7, 100)

  def selectStdDeviations(percentiles: List[Percentile]): List[Percentile] =
    standardDeviations
      .foldLeft(List.empty[Percentile]) { (ps, std) =>
        val maybeP = percentiles.dropWhile(_.percentile < std).headOption
        maybeP.map(_.copy(percentile = std)).toList ::: ps
      }
      .reverse
}
