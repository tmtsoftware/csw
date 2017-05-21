package csw.services.logging

import scala.concurrent.Promise
import akka.actor.{Actor, Props}
import com.persist.Exceptions.SystemException
import org.joda.time.format.ISODateTimeFormat
import com.persist.JsonOps._
import LoggingLevels._

private[logging] object LogActor {

  sealed trait LogActorMessage

  case class LogMessage(level: Level,
                        id: AnyId,
                        time: Long,
                        actorName: Option[String],
                        msg: Json,
                        sourceLocation: SourceLocation,
                        ex: Throwable,
                        kind: String = "")
      extends LogActorMessage

  case class SetLevel(level: Level) extends LogActorMessage

  case class AltMessage(category: String, time: Long, j: JsonObject, id: AnyId, ex: Throwable) extends LogActorMessage

  case class Slf4jMessage(level: Level,
                          time: Long,
                          className: String,
                          msg: String,
                          line: Int,
                          file: String,
                          ex: Throwable)
      extends LogActorMessage

  case class SetSlf4jLevel(level: Level) extends LogActorMessage

  case class AkkaMessage(time: Long, level: Level, source: String, clazz: Class[_], msg: Any, cause: Option[Throwable])
      extends LogActorMessage

  case class SetAkkaLevel(level: Level) extends LogActorMessage

  case object LastAkkaMessage extends LogActorMessage

  case class SetFilter(filter: Option[(JsonObject, Level) => Boolean]) extends LogActorMessage

  case object StopLogging extends LogActorMessage

  def props(done: Promise[Unit],
            standardHeaders: JsonObject,
            appends: Seq[LogAppender],
            initLevel: Level,
            initSlf4jLevel: Level,
            initAkkaLevel: Level) =
    Props(new LogActor(done, standardHeaders, appends, initLevel, initSlf4jLevel, initAkkaLevel))

}

private[logging] class LogActor(done: Promise[Unit],
                                standardHeaders: JsonObject,
                                appenders: Seq[LogAppender],
                                initLevel: Level,
                                initSlf4jLevel: Level,
                                initAkkaLevel: Level)
    extends Actor {

  import LogActor._

  private[this] var filter: Option[(JsonObject, Level) => Boolean] = None
  private[this] var level: Level                                   = initLevel
  private[this] var akkaLogLevel: Level                            = initAkkaLevel
  private[this] var slf4jLogLevel: Level                           = initSlf4jLevel

  private[this] val logFmt = ISODateTimeFormat.dateTime()

  private def exToJson(ex: Throwable): Json = {
    val name = ex.getClass.toString
    ex match {
      case ex: RichException =>
        JsonObject("ex" -> name, "msg" -> ex.richMsg)
      case ex: SystemException =>
        JsonObject("kind" -> ex.kind, "info" -> ex.info)
      case ex: Throwable =>
        JsonObject("ex" -> name, "msg" -> ex.getMessage)
    }
  }

  private def getStack(ex: Throwable): Seq[JsonObject] = {
    val stack = ex.getStackTrace map { trace =>
      val j0 = if (trace.getLineNumber > 0) {
        JsonObject("line" -> trace.getLineNumber)
      } else {
        emptyJsonObject
      }
      val j1 = JsonObject(
        "class"  -> trace.getClassName,
        "file"   -> trace.getFileName,
        "method" -> trace.getMethodName
      )
      j0 ++ j1
    }
    stack
  }

  private def exceptionJson(ex: Throwable): JsonObject = {
    val stack = getStack(ex)
    val j1 = ex match {
      case r: RichException if r.cause != noException =>
        JsonObject("cause" -> exceptionJson(r.cause))
      case ex1: Throwable if ex.getCause != null =>
        JsonObject("cause" -> exceptionJson(ex.getCause))
      case _ => emptyJsonObject
    }
    JsonObject("trace" -> JsonObject("msg" -> exToJson(ex), "stack" -> stack.toSeq)) ++ j1
  }

  private def append(stdHeaders: JsonObject, baseMsg: JsonObject, category: String, level: Level): Unit = {
    val keep = category != "common" || (filter match {
      case Some(f) => f(stdHeaders ++ baseMsg, level)
      case None    => true
    })
    if (keep) {
      for (a <- appenders) a.append(baseMsg, category)
    }
  }

  def receive = {
    case LogMessage(level, id, time, actorName, msg, sourceLocation, ex, kind) =>
      val t = logFmt.print(time)
      val j = JsonObject("@timestamp" -> t, "msg" -> msg, "file" -> sourceLocation.fileName, "@severity" -> level.name,
        "@category" -> "common")
      val j0 = if (sourceLocation.line > 0) {
        JsonObject("line" -> sourceLocation.line)
      } else {
        emptyJsonObject
      }
      val className = (sourceLocation.packageName, sourceLocation.className) match {
        case (p, "") => ""
        case ("", c) => c
        case (p, c)  => s"$p.$c"
      }
      val j1 = if (className == "") {
        emptyJsonObject
      } else {
        JsonObject("class" -> className)
      }
      val j2 = actorName match {
        case Some(actorName) => JsonObject("actor" -> actorName)
        case None            => emptyJsonObject
      }
      val j3 = if (ex == noException) {
        emptyJsonObject
      } else {
        exceptionJson(ex)
      }
      val j4 = id match {
        case RequestId(trackingId, spanId, level) =>
          JsonObject("@traceId" -> JsonArray(trackingId, spanId))
        case noId => emptyJsonObject
      }
      val j5 = if (kind == "") {
        emptyJsonObject
      } else {
        JsonObject("kind" -> kind)
      }
      val shortMsg = j ++ j0 ++ j1 ++ j2 ++ j3 ++ j4 ++ j5
      append(standardHeaders, shortMsg, "common", level)

    case SetLevel(level1) => level = level1

    case AltMessage(category, time, j, id, ex) =>
      val t = logFmt.print(time)
      val j3 = if (ex == noException) {
        emptyJsonObject
      } else {
        exceptionJson(ex)
      }
      val j4 = id match {
        case RequestId(trackingId, spanId, level) =>
          JsonObject("@traceId" -> JsonArray(trackingId, spanId))
        case noId => emptyJsonObject
      }
      append(standardHeaders, j ++ j3 ++ j4 ++ JsonObject("@timestamp" -> t), category, LoggingLevels.INFO)

    case Slf4jMessage(level, time, className, msg, line, file, ex) => {
      if (level.pos >= slf4jLogLevel.pos) {
        val t = logFmt.print(time)
        val j = JsonObject("@timestamp" -> t, "msg" -> msg, "file" -> file, "@severity" -> level.name,
          "class" -> className, "kind" -> "slf4j", "@category" -> "common")
        val j0 = if (line > 0) {
          JsonObject("line" -> line)
        } else {
          emptyJsonObject
        }
        val j1 = if (ex == noException) {
          emptyJsonObject
        } else {
          exceptionJson(ex)
        }
        val shortMsg = j ++ j0 ++ j1
        append(standardHeaders, shortMsg, "common", level)
      }
    }

    case SetSlf4jLevel(level) => slf4jLogLevel = level

    case AkkaMessage(time, level, source, clazz, msg, cause) =>
      if (level.pos >= akkaLogLevel.pos) {
        val msg1 = if (msg == null) "UNKNOWN" else msg
        val t    = logFmt.print(time)
        val j1 = cause match {
          case Some(ex) => exceptionJson(ex)
          case None     => emptyJsonObject
        }
        val shortMsg = JsonObject("@timestamp" -> t, "kind" -> "akka", "msg" -> msg1.toString(), "actor" -> source,
          "@severity" -> level.name, "class" -> clazz.getName(), "@category" -> "common") ++ j1
        append(standardHeaders, shortMsg, "common", level)
      }

    case SetAkkaLevel(level) => akkaLogLevel = level

    case LastAkkaMessage =>
      val akkaLog = akka.event.Logging(context.system, this)
      akkaLog.error("DIE")

    case StopLogging =>
      done.success(())
      context.stop(self)

    case SetFilter(f) => filter = f

    case msg: Any =>
      println(("Unrecognized LogActor message:" + msg + DefaultSourceLocation))
  }
}
