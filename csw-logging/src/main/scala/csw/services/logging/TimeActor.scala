package csw.services.logging

import com.persist.JsonOps._
import scala.collection.mutable
import scala.concurrent.Promise

private[logging] object TimeActorMessages {

  private[logging] trait TimeActorMessage

  private[logging] case class TimeStart(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  private[logging] case class TimeEnd(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  private[logging] case object TimeDone extends TimeActorMessage

}

private[logging] class TimeActor(tdone: Promise[Unit]) extends ActorLogging {

  import TimeActorMessages._

  private case class TimeStep(name: String, start: Long, var end: Long = 0, first: Boolean = false)

  private case class TimeItem(start: Long,
                              steps: mutable.HashMap[String, TimeStep] = mutable.HashMap[String, TimeStep]())

  private val items = mutable.HashMap[String, TimeItem]()

  def start(id: RequestId, time: Long) {
    val key = s"${id.trackingId}\t${id.spanId}"
    items += (key -> TimeItem(time))
  }

  def end(id: RequestId) {
    val key = s"${id.trackingId}\t${id.spanId}"
    items.get(key) map {
      case timeItem =>
        val jitems0 = timeItem.steps map {
          case (key1, timeStep) =>
            val j1 = JsonObject("name" -> timeStep.name, "time0" -> timeStep.start)
            val j2 = if (timeStep.end == 0) {
              emptyJsonObject
            } else {
              JsonObject("time1" -> timeStep.end, "total" -> (timeStep.end - timeStep.start))
            }
            j1 ++ j2
        }
        val traceId = JsonArray(id.trackingId, id.spanId)
        val jitems  = jitems0.toSeq.sortBy(jgetInt(_, "time0"))
        val j       = JsonObject("@traceId" -> traceId, "items" -> jitems)
        log.alternative("time", j)
        items -= key
    }
  }

  def logStart(id: RequestId, name: String, uid: String, time: Long) {
    val key  = s"${id.trackingId}\t${id.spanId}"
    val key1 = s"${name}\t${uid}"
    val first = if (!items.isDefinedAt(key)) {
      start(id, time)
      true
    } else {
      false
    }
    items.get(key) map {
      case timeItem => timeItem.steps += (key1 -> TimeStep(name, time - timeItem.start, first = first))
    }
  }

  def logEnd(id: RequestId, name: String, uid: String, time: Long) {
    val key  = s"${id.trackingId}\t${id.spanId}"
    val key1 = s"${name}\t${uid}"
    items.get(key) map {
      case timeItem =>
        timeItem.steps.get(key1) map {
          case timeStep =>
            timeStep.end = time - timeItem.start
            if (timeStep.first) end(id)
        }
    }
  }

  def closeAll(): Unit =
    for ((key, steps) <- items) {
      val parts = key.split("\t")
      if (parts.size == 2) {
        end(RequestId(parts(0), parts(1)))
      }
    }

  def receive = {
    case TimeStart(id, name, uid, time) =>
      logStart(id, name, uid, time)

    case TimeEnd(id, name, uid, time) =>
      logEnd(id, name, uid, time)

    case TimeDone =>
      closeAll()
      tdone.success(())
      context.stop(self)

    case _ =>
  }

}
