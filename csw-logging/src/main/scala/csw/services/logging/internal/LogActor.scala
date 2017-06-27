package csw.services.logging.internal

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneId}

import akka.actor.{Actor, Props}
import com.persist.Exceptions.SystemException
import com.persist.JsonOps._
import csw.services.logging._
import csw.services.logging.appenders.LogAppender
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.macros.DefaultSourceLocation
import csw.services.logging.scaladsl.{RequestId, RichException}

import scala.concurrent.Promise

/**
 * All log messages are routed to this single Akka Actor. There is one LogActor per logging system.
 * Logging messages from logging API, Java Slf4j and Akka loggers are sent to this actor.
 */
private[logging] object LogActor {

  def props(done: Promise[Unit],
            standardHeaders: JsonObject,
            appends: Seq[LogAppender],
            initLevel: Level,
            initSlf4jLevel: Level,
            initAkkaLevel: Level): Props =
    Props(new LogActor(done, standardHeaders, appends, initLevel, initSlf4jLevel, initAkkaLevel))

}

private[logging] class LogActor(done: Promise[Unit],
                                standardHeaders: JsonObject,
                                appenders: Seq[LogAppender],
                                initLevel: Level,
                                initSlf4jLevel: Level,
                                initAkkaLevel: Level)
    extends Actor {

  private[this] var filter: Option[(JsonObject, Level) => Boolean] = None
  private[this] var level: Level                                   = initLevel
  private[this] var akkaLogLevel: Level                            = initAkkaLevel
  private[this] var slf4jLogLevel: Level                           = initSlf4jLevel

  private[this] val ISOLogFmt =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSxxxxx")

  def receive: Receive = {
    case log: Log                     => receiveLog(log)
    case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage)
    case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J)
    case logAkka: LogAkka             => receiveLogAkkaMessage(logAkka)
    case SetLevel(level1)             => level = level1
    case SetSlf4jLevel(level1)        => slf4jLogLevel = level1
    case SetAkkaLevel(level1)         => akkaLogLevel = level1
    case SetFilter(f)                 => filter = f
    case LastAkkaMessage =>
      akka.event.Logging(context.system, this).error("DIE")
    case StopLogging =>
      context.stop(self)
      done.success(())
    case msg: Any =>
      println("Unrecognized LogActor message:" + msg + DefaultSourceLocation)
  }

  private def exToJson(ex: Throwable): Json = {
    val name = ex.getClass.toString
    ex match {
      case ex: RichException =>
        JsonObject("ex" -> name, "message" -> ex.richMsg)
      case ex: SystemException =>
        JsonObject("kind" -> ex.kind, "info" -> ex.info)
      case ex: Throwable =>
        JsonObject("ex" -> name, "message" -> ex.getMessage)
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
    JsonObject("trace" -> JsonObject("message" -> exToJson(ex), "stack" -> stack)) ++ j1
  }

  private def append(baseMsg: JsonObject, category: String, level: Level): Unit = {
    val keep = category != "common" || (filter match {
      case Some(f) => f(standardHeaders ++ baseMsg, level)
      case None    => true
    })
    if (keep) {
      for (appender <- appenders) appender.append(baseMsg, category)
    }
  }

  private def receiveLog(log: Log): Unit = {
    var jsonObject = JsonObject("timestamp" -> formatLogTimeToISOFmt(log.time), "message" -> log.sanitizedMessage,
      "@severity" -> log.level.name, "@category" -> "common")

    if (!log.sourceLocation.fileName.isEmpty) {
      jsonObject = jsonObject ++ JsonObject("file" -> log.sourceLocation.fileName)
    }

    if (log.sourceLocation.line > 0)
      jsonObject = jsonObject ++ JsonObject("line" -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ JsonObject("class" -> c)
      case (p, c)   ⇒ jsonObject ++ JsonObject("class" -> s"$p.$c")
    }

    if (log.actorName.isDefined)
      jsonObject = jsonObject ++ JsonObject("actor" -> log.actorName.get)

    if (log.componentName.isDefined)
      jsonObject = jsonObject ++ JsonObject("@componentName" -> log.componentName.get)

    if (log.ex != noException) jsonObject = jsonObject ++ exceptionJson(log.ex)
    jsonObject = log.id match {
      case RequestId(trackingId, spanId, _) ⇒
        jsonObject ++ JsonObject("@traceId" -> JsonArray(trackingId, spanId))
      case _ ⇒ jsonObject
    }
    if (!log.kind.isEmpty)
      jsonObject = jsonObject ++ JsonObject("kind" -> log.kind)
    append(jsonObject, "common", log.level)
  }

  private def receiveAltMessage(logAltMessage: LogAltMessage) = {
    var jsonObject = logAltMessage.jsonObject
    if (logAltMessage.ex != noException)
      jsonObject = jsonObject ++ exceptionJson(logAltMessage.ex)
    jsonObject = logAltMessage.id match {
      case RequestId(trackingId, spanId, _) =>
        jsonObject ++ JsonObject("@traceId" -> JsonArray(trackingId, spanId))
      case _ => jsonObject
    }
    jsonObject = jsonObject ++ JsonObject("timestamp" -> formatLogTimeToISOFmt(logAltMessage.time))
    append(jsonObject, logAltMessage.category, LoggingLevels.INFO)
  }

  private def receiveLogSlf4j(logSlf4j: LogSlf4j) =
    if (logSlf4j.level.pos >= slf4jLogLevel.pos) {
      var jsonObject = JsonObject(
        "timestamp" -> formatLogTimeToISOFmt(logSlf4j.time),
        "message"   -> logSlf4j.msg,
        "file"      -> logSlf4j.file,
        "@severity" -> logSlf4j.level.name,
        "class"     -> logSlf4j.className,
        "kind"      -> "slf4j",
        "@category" -> "common"
      )
      if (logSlf4j.line > 0)
        jsonObject = jsonObject ++ JsonObject("line" -> logSlf4j.line)
      if (logSlf4j.ex != noException)
        jsonObject = jsonObject ++ exceptionJson(logSlf4j.ex)
      append(jsonObject, "common", logSlf4j.level)
    }

  private def receiveLogAkkaMessage(logAkka: LogAkka) =
    if (logAkka.level.pos >= akkaLogLevel.pos) {
      val msg1 = if (logAkka.msg.toString.isEmpty) "UNKNOWN" else logAkka.msg
      var jsonObject = JsonObject(
        "timestamp" -> formatLogTimeToISOFmt(logAkka.time),
        "kind"      -> "akka",
        "message"   -> msg1.toString,
        "actor"     -> logAkka.source,
        "@severity" -> logAkka.level.name,
        "class"     -> logAkka.clazz.getName,
        "@category" -> "common"
      )

      if (logAkka.cause.isDefined)
        jsonObject = jsonObject ++ exceptionJson(logAkka.cause.get)
      append(jsonObject, "common", logAkka.level)
    }

  private def formatLogTimeToISOFmt(time: Long) = {
    val offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault)
    offsetDateTime.format(ISOLogFmt)
  }
}
