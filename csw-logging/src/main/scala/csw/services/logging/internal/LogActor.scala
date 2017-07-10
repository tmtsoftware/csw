package csw.services.logging.internal

import akka.actor.{Actor, Props}
import com.persist.Exceptions.SystemException
import com.persist.JsonOps._
import csw.services.logging._
import csw.services.logging.appenders.LogAppender
import csw.services.logging.commons.{Category, Keys, TMTDateTimeFormatter}
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

  private[this] var level: Level                = initLevel
  private[this] var akkaLogLevel: Level         = initAkkaLevel
  private[this] var slf4jLogLevel: Level        = initSlf4jLevel
  private[this] var appenders: Seq[LogAppender] = initAppenders

  def receive: Receive = {
    case log: Log                     => receiveLog(log)
    case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage)
    case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J)
    case logAkka: LogAkka             => receiveLogAkkaMessage(logAkka)
    case SetLevel(level1)             => level = level1
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
        JsonObject(Keys.EX -> name, Keys.MESSAGE -> ex.richMsg)
      case ex: SystemException =>
        JsonObject(Keys.KIND -> ex.kind, "info" -> ex.info)
      case ex: Throwable =>
        JsonObject(Keys.EX -> name, Keys.MESSAGE -> ex.getMessage)
    }
  }

  // Convert exception stack trace to JSON
  private def getStack(ex: Throwable): Seq[JsonObject] = {
    val stack = ex.getStackTrace map { trace =>
      val j0 = if (trace.getLineNumber > 0) {
        JsonObject(Keys.LINE -> trace.getLineNumber)
      } else {
        emptyJsonObject
      }
      val j1 = JsonObject(
        Keys.CLASS  -> trace.getClassName,
        Keys.FILE   -> trace.getFileName,
        Keys.METHOD -> trace.getMethodName
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
        JsonObject(Keys.CAUSE -> exceptionJson(r.cause))
      case ex1: Throwable if ex.getCause != null =>
        JsonObject(Keys.CAUSE -> exceptionJson(ex.getCause))
      case _ => emptyJsonObject
    }
    JsonObject(Keys.TRACE -> JsonObject(Keys.MESSAGE -> exToJson(ex), Keys.STACK -> stack)) ++ j1
  }

  // Send JSON log object for each appender configured for the logging system
  private def append(baseMsg: JsonObject, category: String, level: Level): Unit =
    for (appender <- appenders) appender.append(baseMsg, category)

  private def receiveLog(log: Log): Unit = {

    val msg = if (log.map.isEmpty) log.msg else Map(Keys.MSG → log.msg) ++ log.map

    var jsonObject = JsonObject(Keys.TIMESTAMP -> TMTDateTimeFormatter.format(log.time), Keys.MESSAGE → msg,
      Keys.SEVERITY -> log.level.name, Keys.CATEGORY -> Category.Common.name)

    if (!log.sourceLocation.fileName.isEmpty) {
      jsonObject = jsonObject ++ JsonObject(Keys.FILE -> log.sourceLocation.fileName)
    }

    if (log.sourceLocation.line > 0)
      jsonObject = jsonObject ++ JsonObject(Keys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ JsonObject(Keys.CLASS -> c)
      case (p, c)   ⇒ jsonObject ++ JsonObject(Keys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined)
      jsonObject = jsonObject ++ JsonObject(Keys.ACTOR -> log.actorName.get)

    if (log.componentName.isDefined)
      jsonObject = jsonObject ++ JsonObject(Keys.COMPONENT_NAME -> log.componentName.get)

    if (log.ex != noException) jsonObject = jsonObject ++ exceptionJson(log.ex)
    jsonObject = log.id match {
      case RequestId(trackingId, spanId, _) ⇒
        jsonObject ++ JsonObject(Keys.TRACE_ID -> JsonArray(trackingId, spanId))
      case _ ⇒ jsonObject
    }
    if (!log.kind.isEmpty)
      jsonObject = jsonObject ++ JsonObject(Keys.KIND -> log.kind)
    append(jsonObject, Category.Common.name, log.level)
  }

  private def receiveAltMessage(logAltMessage: LogAltMessage) = {
    var jsonObject = logAltMessage.jsonObject
    if (logAltMessage.ex != noException)
      jsonObject = jsonObject ++ exceptionJson(logAltMessage.ex)
    jsonObject = logAltMessage.id match {
      case RequestId(trackingId, spanId, _) =>
        jsonObject ++ JsonObject(Keys.TRACE_ID -> JsonArray(trackingId, spanId))
      case _ => jsonObject
    }
    jsonObject = jsonObject ++ JsonObject(Keys.TIMESTAMP -> TMTDateTimeFormatter.format(logAltMessage.time))
    append(jsonObject, logAltMessage.category, LoggingLevels.INFO)
  }

  private def receiveLogSlf4j(logSlf4j: LogSlf4j) =
    if (logSlf4j.level.pos >= slf4jLogLevel.pos) {
      var jsonObject = JsonObject(
        Keys.TIMESTAMP -> TMTDateTimeFormatter.format(logSlf4j.time),
        Keys.MESSAGE   -> logSlf4j.msg,
        Keys.FILE      -> logSlf4j.file,
        Keys.SEVERITY  -> logSlf4j.level.name,
        Keys.CLASS     -> logSlf4j.className,
        Keys.KIND      -> "slf4j",
        Keys.CATEGORY  -> Category.Common.name
      )
      if (logSlf4j.line > 0)
        jsonObject = jsonObject ++ JsonObject(Keys.LINE -> logSlf4j.line)
      if (logSlf4j.ex != noException)
        jsonObject = jsonObject ++ exceptionJson(logSlf4j.ex)
      append(jsonObject, Category.Common.name, logSlf4j.level)
    }

  private def receiveLogAkkaMessage(logAkka: LogAkka) =
    if (logAkka.level.pos >= akkaLogLevel.pos) {
      val msg1 = if (logAkka.msg.toString.isEmpty) "UNKNOWN" else logAkka.msg
      var jsonObject = JsonObject(
        Keys.TIMESTAMP -> TMTDateTimeFormatter.format(logAkka.time),
        Keys.KIND      -> "akka",
        Keys.MESSAGE   -> msg1.toString,
        Keys.ACTOR     -> logAkka.source,
        Keys.SEVERITY  -> logAkka.level.name,
        Keys.CLASS     -> logAkka.clazz.getName,
        Keys.CATEGORY  -> Category.Common.name
      )

      if (logAkka.cause.isDefined)
        jsonObject = jsonObject ++ exceptionJson(logAkka.cause.get)
      append(jsonObject, Category.Common.name, logAkka.level)
    }
}
