package csw.services.event.helpers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import csw.messages.events.Event

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class Monitor(tickDuration: FiniteDuration, reportingDuration: FiniteDuration) {

  val cumulative: Flow[Event, Event, NotUsed] = flow(cumulative = true)
  val resetting: Flow[Event, Event, NotUsed]  = flow(cumulative = false)

  private def flow(cumulative: Boolean) = Flow[Event].statefulMapConcat { () =>
    val mode = if (cumulative) "Cumulative" else "Resetting"
    println()
    println(s"--------Starting run. Mode: $mode, TickDuration: $tickDuration, ReportingDuration: $reportingDuration----------")

    var lastId          = 0
    var printTime       = 0L
    var count           = 0
    var outOfOrderCount = 0
    var lastCurrentId   = 0
    var latencies       = newMap()
    var startTime       = System.currentTimeMillis()

    var tickInterval: Interval = Interval.zero

    event =>
      val eventTime   = event.eventTime.time.toEpochMilli
      val currentTime = System.currentTimeMillis
      val latency     = currentTime - eventTime
      val accTime     = currentTime - startTime
      val shouldPrint = (accTime - printTime) > reportingDuration.toMillis
      val currentId   = event.eventId.id.toInt
      val inOrder     = currentId >= lastId

      lastId = currentId

      count += 1

      if (!inOrder) {
        outOfOrderCount += 1
      }

      latencies(latency) += 1

      tickInterval = tickInterval.next(currentTime)

      def throughput(cnt: Int) = if (accTime != 0) ((cnt * 1000) / accTime).toString else "unknown"

      def droppedCount = (currentId - lastCurrentId) - count

      if (shouldPrint) {
        val T        = s"T:${throughput(count)}"
        val L        = s"L:${Percentile.format(latencies, count)}"
        val ord      = s"ord:$outOfOrderCount"
        val droppedT = s"dropT:${throughput(droppedCount)}"

        val tickI = tickInterval.format("tickI")

        // can be added below to print ordering issues
        println(
          f"$T%-6s $L%-30s $droppedT%-12s $ord%-10s $tickI%-45s"
        )
        if (cumulative) {
          printTime = accTime
        } else {
          count = 0
          outOfOrderCount = 0
          lastCurrentId = currentId
          startTime = currentTime
          latencies = newMap()
          tickInterval = tickInterval.reset()
        }
      }

      List(event)
  }

  private def newMap() = mutable.Map.empty[Long, Long].withDefaultValue(0)

  case class Interval(current: Long, last: Long, count: Int, accTime: Long, min: Long, max: Long, error: Double) {
    val length: Long         = current - last
    val drift: Long          = length - tickDuration.toMillis
    val squaredError: Long   = drift * drift
    def avg: Double          = accTime.toDouble / count
    def RMSE: Double         = math.sqrt(error / count) //root mean squared error
    def format(name: String) = f"$name:[avg:$avg%2.1f, min:$min, max:$max, err:$RMSE%2.1f]"
    def next(newCurrent: Long) =
      Interval(newCurrent, current, count + 1, accTime + length, min.min(length), max.max(length), error + squaredError)
    def reset(): Interval = copy(count = 0, accTime = 0, min = Int.MaxValue, max = 0, error = 0)
  }

  object Interval {
    def zero: Interval = {
      val initialCurrent = System.currentTimeMillis() - tickDuration.toMillis
      Interval(current = initialCurrent, last = 0, count = 0, accTime = 0, min = Int.MaxValue, max = 0, error = 0)
    }
  }
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
  private val standardDeviations = List(95.5, 99.7)

  private def selectStdDeviations(percentiles: List[Percentile]): List[Percentile] =
    standardDeviations
      .foldLeft(List.empty[Percentile]) { (ps, std) =>
        val maybeP = percentiles.dropWhile(_.percentile < std).headOption
        maybeP.map(_.copy(percentile = std)).toList ::: ps
      }
      .reverse

  def format(latencies: mutable.Map[Long, Long], count: Long): String = {
    val percentiles    = Latency.accumulate(latencies).map(_.percentile(count))
    val stdPercentiles = Percentile.selectStdDeviations(percentiles)
    stdPercentiles.map(p => s"${f"${p.percentile}%2.1f"} -> ${p.latency}").mkString("[", ", ", "]")
  }
}
