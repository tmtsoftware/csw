package csw.services.event.internal.perf

import akka.NotUsed
import akka.stream.scaladsl.Flow
import csw.messages.ccs.events.Event

object Monitor {
  private val buckets = List(3, 5, 7, 9)

  val cumulative: Flow[Event, Event, NotUsed] = flow(cumulative = true)
  val resetting: Flow[Event, Event, NotUsed]  = flow(cumulative = false)

  private def flow(cumulative: Boolean) = Flow[Event].statefulMapConcat { () =>
    var lastId = 0

    var printTime = 0L
    var count     = 0

    var startTime       = System.currentTimeMillis()
    var latencies       = newMap()
    var outOfOrderCount = 0
    var lastCurrentId   = 0

    event =>
      val eventTime    = event.eventTime.time.toEpochMilli
      val currentTime  = System.currentTimeMillis
      val latency      = currentTime - eventTime
      val accTime      = currentTime - startTime
      val shouldPrint  = (accTime - printTime) > 2000
      val currentId    = event.eventId.id.toInt
      val isOutOfOrder = (currentId - lastId) < 1

      lastId = currentId

      count += 1

      if (isOutOfOrder) {
        outOfOrderCount += 1
      }

      buckets.foreach { x =>
        if (latency <= x) {
          latencies(x) += 1
        }
      }

      def throughput = if (accTime != 0) ((count * 1000) / accTime).toString else "unknown"

      def latenciesStr = latencies.toList.sortBy(_._1).map {
        case (k, v) =>
          val percentile = v * 100.0 / count
          s"${k}ms -> ${f"$percentile%2.2f"}"
      }

      def droppedCount = (currentId - lastCurrentId) - count

      if (shouldPrint) {
        println(s"throughput:$throughput    latencies:$latenciesStr     out-of-order:$outOfOrderCount     dropped:$droppedCount")
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

  private def newMap() = collection.mutable.Map.empty[Long, Long].withDefaultValue(0)

}
