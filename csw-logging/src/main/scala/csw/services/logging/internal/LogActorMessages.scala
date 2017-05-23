package csw.services.logging.internal

import com.persist.JsonOps.{Json, JsonObject}
import LoggingLevels.Level
import csw.services.logging.SourceLocation
import csw.services.logging.scaladsl.AnyId

sealed trait LogActorMessages

case class Log(componentName: Option[String],
               level: Level,
               id: AnyId,
               time: Long,
               actorName: Option[String],
               msg: Json,
               sourceLocation: SourceLocation,
               ex: Throwable,
               kind: String = "")
    extends LogActorMessages

case class SetLevel(level: Level) extends LogActorMessages

case class LogAltMessage(category: String, time: Long, jsonObject: JsonObject, id: AnyId, ex: Throwable)
    extends LogActorMessages

case class LogSlf4j(level: Level, time: Long, className: String, msg: String, line: Int, file: String, ex: Throwable)
    extends LogActorMessages

case class SetSlf4jLevel(level: Level) extends LogActorMessages

case class LogAkka(time: Long, level: Level, source: String, clazz: Class[_], msg: Any, cause: Option[Throwable])
    extends LogActorMessages

case class SetAkkaLevel(level: Level) extends LogActorMessages

case object LastAkkaMessage extends LogActorMessages

case class SetFilter(filter: Option[(JsonObject, Level) => Boolean]) extends LogActorMessages

case object StopLogging extends LogActorMessages
