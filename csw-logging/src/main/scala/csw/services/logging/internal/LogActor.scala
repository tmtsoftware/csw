package csw.services.logging.internal

import akka.actor.{Actor, Props}
import com.persist.Exceptions.SystemException
import com.persist.JsonOps._
import csw.services.logging._
import csw.services.logging.appenders.LogAppender
import csw.services.logging.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
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
                                initAppenders: Seq[LogAppender],
                                initLevel: Level,
                                initSlf4jLevel: Level,
                                initAkkaLevel: Level)
    extends Actor {

  private[this] var akkaLogLevel: Level         = initAkkaLevel
  private[this] var slf4jLogLevel: Level        = initSlf4jLevel
  private[this] var appenders: Seq[LogAppender] = initAppenders

  def receive: Receive = {
    case log: Log                     => receiveLog(log)
    case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage)
    case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J)
    case logAkka: LogAkka             => receiveLogAkkaMessage(logAkka)
    case SetSlf4jLevel(level1)        => slf4jLogLevel = level1
    case SetAkkaLevel(level1)         => akkaLogLevel = level1
    case SetAppenders(_appenders)     => appenders = _appenders
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
        JsonObject(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.richMsg)
      case ex: SystemException =>
        JsonObject(LoggingKeys.KIND -> ex.kind, "info" -> ex.info)
      case ex: Throwable =>
        JsonObject(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.getMessage)
    }
  }

  // Convert exception stack trace to JSON
  private def getStack(ex: Throwable): Seq[JsonObject] = {
    val stack = ex.getStackTrace map { trace =>
      val j0 = if (trace.getLineNumber > 0) {
        JsonObject(LoggingKeys.LINE -> trace.getLineNumber)
      } else {
        emptyJsonObject
      }
      val j1 = JsonObject(
        LoggingKeys.CLASS  -> trace.getClassName,
        LoggingKeys.FILE   -> trace.getFileName,
        LoggingKeys.METHOD -> trace.getMethodName
      )
      j0 ++ j1
    }
    stack
  }

  // Convert exception to JSON
  private def exceptionJson(ex: Throwable): JsonObject = {
    val stack = getStack(ex)
    val j1 = ex match {
      case r: RichException if r.cause != noException =>
        JsonObject(LoggingKeys.CAUSE -> exceptionJson(r.cause))
      case ex1: Throwable if ex.getCause != null =>
        JsonObject(LoggingKeys.CAUSE -> exceptionJson(ex.getCause))
      case _ => emptyJsonObject
    }
    JsonObject(LoggingKeys.TRACE -> JsonObject(LoggingKeys.MESSAGE -> exToJson(ex), LoggingKeys.STACK -> stack)) ++ j1
  }

  // Send JSON log object for each appender configured for the logging system
  private def append(baseMsg: JsonObject, category: String, level: Level): Unit =
    for (appender <- appenders) appender.append(baseMsg, category)

  private def receiveLog(log: Log): Unit = {

    var jsonObject = JsonObject(
      LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(log.time),
      LoggingKeys.MESSAGE   → log.msg,
      LoggingKeys.SEVERITY  -> log.level.name,
      LoggingKeys.CATEGORY  -> Category.Common.name
    )

    // This lime adds the user map objects as additional JsonObjects if the map is not empty
    jsonObject = jsonObject ++ log.map

    if (!log.sourceLocation.fileName.isEmpty) {
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.FILE -> log.sourceLocation.fileName)
    }

    if (log.sourceLocation.line > 0)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ JsonObject(LoggingKeys.CLASS -> c)
      case (p, c)   ⇒ jsonObject ++ JsonObject(LoggingKeys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.ACTOR -> log.actorName.get)

    if (log.componentName.isDefined)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.COMPONENT_NAME -> log.componentName.get)

    if (log.ex != noException) jsonObject = jsonObject ++ exceptionJson(log.ex)
    jsonObject = log.id match {
      case RequestId(trackingId, spanId, _) ⇒
        jsonObject ++ JsonObject(LoggingKeys.TRACE_ID -> JsonArray(trackingId, spanId))
      case _ ⇒ jsonObject
    }
    if (!log.kind.isEmpty)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.KIND -> log.kind)
    append(jsonObject, Category.Common.name, log.level)
  }

  private def receiveAltMessage(logAltMessage: LogAltMessage) = {
    var jsonObject = logAltMessage.jsonObject
    if (logAltMessage.ex != noException)
      jsonObject = jsonObject ++ exceptionJson(logAltMessage.ex)
    jsonObject = logAltMessage.id match {
      case RequestId(trackingId, spanId, _) =>
        jsonObject ++ JsonObject(LoggingKeys.TRACE_ID -> JsonArray(trackingId, spanId))
      case _ => jsonObject
    }
    jsonObject = jsonObject ++ JsonObject(LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logAltMessage.time))
    append(jsonObject, logAltMessage.category, LoggingLevels.INFO)
  }

  private def receiveLogSlf4j(logSlf4j: LogSlf4j) =
    if (logSlf4j.level.pos >= slf4jLogLevel.pos) {
      var jsonObject = JsonObject(
        LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logSlf4j.time),
        LoggingKeys.MESSAGE   -> logSlf4j.msg,
        LoggingKeys.FILE      -> logSlf4j.file,
        LoggingKeys.SEVERITY  -> logSlf4j.level.name,
        LoggingKeys.CLASS     -> logSlf4j.className,
        LoggingKeys.KIND      -> "slf4j",
        LoggingKeys.CATEGORY  -> Category.Common.name
      )
      if (logSlf4j.line > 0)
        jsonObject = jsonObject ++ JsonObject(LoggingKeys.LINE -> logSlf4j.line)
      if (logSlf4j.ex != noException)
        jsonObject = jsonObject ++ exceptionJson(logSlf4j.ex)
      append(jsonObject, Category.Common.name, logSlf4j.level)
    }

  private def receiveLogAkkaMessage(logAkka: LogAkka) =
    if (logAkka.level.pos >= akkaLogLevel.pos) {
      val msg1 = if (logAkka.msg.toString.isEmpty) "UNKNOWN" else logAkka.msg
      var jsonObject = JsonObject(
        LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logAkka.time),
        LoggingKeys.KIND      -> "akka",
        LoggingKeys.MESSAGE   -> msg1.toString,
        LoggingKeys.ACTOR     -> logAkka.source,
        LoggingKeys.SEVERITY  -> logAkka.level.name,
        LoggingKeys.CLASS     -> logAkka.clazz.getName,
        LoggingKeys.CATEGORY  -> Category.Common.name
      )

      if (logAkka.cause.isDefined)
        jsonObject = jsonObject ++ exceptionJson(logAkka.cause.get)
      append(jsonObject, Category.Common.name, logAkka.level)
    }
}
