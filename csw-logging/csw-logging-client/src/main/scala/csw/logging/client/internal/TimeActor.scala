package csw.logging.client.internal

import akka.actor.Actor
import csw.logging.api.models.RequestId
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.LoggingKeys
import csw.logging.client.internal.JsonExtensions.{AnyToJson, RichJsObject}
import csw.logging.client.scaladsl.GenericLoggerFactory
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Promise

/**
 * Actor responsible for writing logs which time a functional block
 *
 * @param tdone a promise to let the caller know when this actors responsibility is done
 */
private[logging] class TimeActor(tdone: Promise[Unit]) extends Actor {

  private val log: Logger = GenericLoggerFactory.getLogger(context)

  import TimeActorMessages._

  private case class TimeStep(name: String, start: Long, var end: Long = 0, first: Boolean = false)

  private case class TimeItem(start: Long, steps: mutable.HashMap[String, TimeStep] = mutable.HashMap[String, TimeStep]())

  private val items = mutable.HashMap[String, TimeItem]()

  def start(id: RequestId, time: Long): Unit = {
    val key = s"${id.trackingId}\t${id.spanId}"
    items += (key -> TimeItem(time))
  }

  def end(id: RequestId): Unit = {
    val key = s"${id.trackingId}\t${id.spanId}"
    items.get(key) map { timeItem =>
      val jitems0 = timeItem.steps map {
        case (key1, timeStep) =>
          val j1 = Json.obj("name" -> timeStep.name, "time0" -> timeStep.start)
          val j2 = if (timeStep.end == 0) {
            Json.obj()
          } else {
            Json.obj("time1" -> timeStep.end, "total" -> (timeStep.end - timeStep.start))
          }
          j1 ++ j2
      }
      val traceId = Seq(id.trackingId, id.spanId).asJson
      val jitems  = jitems0.toSeq.sortBy(_.getString("time0"))
      val j       = Map(LoggingKeys.TRACE_ID -> traceId, "items" -> jitems)
      log.alternative("time", j)
      items -= key
    }
  }

  def logStart(id: RequestId, name: String, uid: String, time: Long): Unit = {
    val key  = s"${id.trackingId}\t${id.spanId}"
    val key1 = s"$name\t$uid"
    val first = if (!items.isDefinedAt(key)) {
      start(id, time)
      true
    } else {
      false
    }
    items.get(key) map (timeItem => timeItem.steps += (key1 -> TimeStep(name, time - timeItem.start, first = first)))
  }

  def logEnd(id: RequestId, name: String, uid: String, time: Long): Unit = {
    val key  = s"${id.trackingId}\t${id.spanId}"
    val key1 = s"$name\t$uid"
    items.get(key) foreach { timeItem =>
      timeItem.steps.get(key1) foreach { timeStep =>
        timeStep.end = time - timeItem.start
        if (timeStep.first) end(id)
      }
    }
  }

  def closeAll(): Unit =
    for ((key, _) <- items) {
      val parts = key.split("\t")
      if (parts.size == 2) end(RequestId(parts(0), parts(1)))
    }

  def receive: Receive = {
    case TimeStart(id, name, uid, time) => logStart(id, name, uid, time)
    case TimeEnd(id, name, uid, time)   => logEnd(id, name, uid, time)
    case TimeDone =>
      closeAll()
      tdone.success(())
      context.stop(self)
    case _ =>
  }

}
