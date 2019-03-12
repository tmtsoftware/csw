package csw.logging.client.internal

import java.io.{PrintWriter, StringWriter}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.event.LogSource
import csw.logging.api._
import csw.logging.api.models.LoggingLevels.Level
import csw.logging.api.models.{LoggingLevels, RequestId}
import csw.logging.client.appenders.LogAppender
import csw.logging.client.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.internal.JsonExtensions.AnyToJson
import csw.logging.client.internal.LogActorMessages._
import csw.logging.client.scaladsl.RichException
import csw.logging.macros.DefaultSourceLocation
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Promise

/**
 * All log messages are routed to this single Akka Actor. There is one LogActor per logging system.
 * Logging messages from logging API, Java Slf4j and Akka loggers are sent to this actor.
 * Messages are then forwarded to one or more configured appenders.
 */
private[logging] object LogActor extends RequiresMessageQueue[BoundedMessageQueueSemantics] {

  def behavior(
      done: Promise[Unit],
      standardHeaders: JsObject,
      appends: Seq[LogAppender],
      initLevel: Level,
      initSlf4jLevel: Level,
      initAkkaLevel: Level
  ): Behavior[LogActorMessages] =
    Behaviors.setup { ctx =>
      import LogActorOperations._

      implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
        def genString(o: AnyRef): String           = o.getClass.getName
        override def getClazz(o: AnyRef): Class[_] = o.getClass
      }

      var level: Level                = initLevel
      var akkaLogLevel: Level         = initAkkaLevel
      var slf4jLogLevel: Level        = initSlf4jLevel
      var appenders: Seq[LogAppender] = appends

      // Send JSON log object for each appender configured for the logging system
      def append(baseMsg: JsObject, category: String, level: Level): Unit =
        for (appender <- appenders) appender.append(baseMsg, category)

      def receiveAltMessage(logAltMessage: LogAltMessage): Unit = {
        var jsonObject = logAltMessage.jsonObject
        if (logAltMessage.ex != NoLogException) jsonObject = jsonObject ++ exceptionJson(logAltMessage.ex)

        jsonObject = logAltMessage.id match {
          case RequestId(trackingId, spanId, _) => jsonObject ++ Json.obj(LoggingKeys.TRACE_ID -> Seq(trackingId, spanId))
          case _                                => jsonObject
        }

        jsonObject = jsonObject ++ Json.obj(LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logAltMessage.time))
        append(jsonObject, logAltMessage.category, LoggingLevels.INFO)
      }

      def receiveLogSlf4j(logSlf4j: LogSlf4j): Unit =
        if (logSlf4j.level.pos >= slf4jLogLevel.pos) {
          var jsonObject = Json.obj(
            LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logSlf4j.time),
            LoggingKeys.MESSAGE   -> logSlf4j.msg,
            LoggingKeys.FILE      -> logSlf4j.file,
            LoggingKeys.SEVERITY  -> logSlf4j.level.name,
            LoggingKeys.CLASS     -> logSlf4j.className,
            LoggingKeys.KIND      -> "slf4j",
            LoggingKeys.CATEGORY  -> Category.Common.name
          )
          if (logSlf4j.line > 0) jsonObject = jsonObject ++ Json.obj(LoggingKeys.LINE -> logSlf4j.line)
          if (logSlf4j.ex != NoLogException) jsonObject = jsonObject ++ exceptionJson(logSlf4j.ex)
          append(jsonObject, Category.Common.name, logSlf4j.level)
        }

      def receiveLogAkkaMessage(logAkka: LogAkka): Unit =
        if (logAkka.level.pos >= akkaLogLevel.pos) {
          val msg1 = if (logAkka.msg == null) "UNKNOWN" else logAkka.msg
          var jsonObject = Json.obj(
            LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logAkka.time),
            LoggingKeys.KIND      -> "akka",
            LoggingKeys.MESSAGE   -> msg1.toString,
            LoggingKeys.ACTOR     -> logAkka.source,
            LoggingKeys.SEVERITY  -> logAkka.level.name,
            LoggingKeys.CLASS     -> logAkka.clazz.getName,
            LoggingKeys.CATEGORY  -> Category.Common.name
          )

          if (logAkka.cause.isDefined) jsonObject = jsonObject ++ exceptionJson(logAkka.cause.get)
          append(jsonObject, Category.Common.name, logAkka.level)
        }

      def receiveLog(log: Log): Unit = append(createJsonFromLog(log), Category.Common.name, log.level)

      Behaviors.receiveMessage[LogActorMessages] { message =>
        message match {
          case log: Log                     => receiveLog(log)
          case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage)
          case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J)
          case logAkka: LogAkka             => receiveLogAkkaMessage(logAkka)
          case SetLevel(level1)             => level = level1
          case SetSlf4jLevel(level1)        => slf4jLogLevel = level1
          case SetAkkaLevel(level1)         => akkaLogLevel = level1
          case SetAppenders(_appenders)     => appenders = _appenders
          case LastAkkaMessage              => akka.event.Logging(ctx.system.toUntyped, this).error("DIE")
          case StopLogging =>
            ctx.stop(ctx.self)
            done.success(())
          case msg: Any => println("Unrecognized LogActor message:" + msg + DefaultSourceLocation)
        }
        Behaviors.same
      }
    }
}

private[logging] object LogActorOperations {
  def exToJson(ex: Throwable): JsObject = {
    val name = ex.getClass.toString
    ex match {
      case ex: RichException => Json.obj(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.richMsg.asJson)
      case ex: Throwable     => Json.obj(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.getMessage)
    }
  }

  // Convert exception stack trace to JSON
  def getStack(ex: Throwable): Seq[JsObject] = {
    val stack = ex.getStackTrace map { trace =>
      val j0 =
        if (trace.getLineNumber > 0) Json.obj(LoggingKeys.LINE -> trace.getLineNumber)
        else Json.obj()
      val j1 = Json.obj(
        LoggingKeys.CLASS  -> trace.getClassName,
        LoggingKeys.FILE   -> trace.getFileName,
        LoggingKeys.METHOD -> trace.getMethodName
      )
      j0 ++ j1
    }
    stack
  }

  // Convert exception to JSON
  def exceptionJson(ex: Throwable): JsObject = {
    val stack = getStack(ex)
    val j1 = ex match {
      case r: RichException if r.cause != NoLogException => Json.obj(LoggingKeys.CAUSE -> exceptionJson(r.cause))
      case ex1: Throwable if ex.getCause != null         => Json.obj(LoggingKeys.CAUSE -> exceptionJson(ex.getCause))
      case _                                             => Json.obj()
    }

    Json.obj(
      LoggingKeys.TRACE -> Json.obj(
        LoggingKeys.MESSAGE -> exToJson(ex),
        LoggingKeys.STACK   -> stack
      ),
      LoggingKeys.PLAINSTACK → extractPlainStacktrace(ex)
    ) ++ j1
  }

  def extractPlainStacktrace(ex: Throwable): String = {
    val sw = new StringWriter()
    ex.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  def createJsonFromLog(log: Log): JsObject = {

    var jsonObject = Json.obj(
      LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(log.time),
      LoggingKeys.MESSAGE   → log.msg,
      LoggingKeys.SEVERITY  -> log.level.name,
      LoggingKeys.CATEGORY  -> Category.Common.name
    )

    // This lime adds the user map objects as additional JsonObjects if the map is not empty
    jsonObject = jsonObject ++ log.map

    if (!log.sourceLocation.fileName.isEmpty) jsonObject = jsonObject ++ Json.obj(LoggingKeys.FILE -> log.sourceLocation.fileName)

    if (log.sourceLocation.line > 0) jsonObject = jsonObject ++ Json.obj(LoggingKeys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ Json.obj(LoggingKeys.CLASS -> c)
      case (p, c)   ⇒ jsonObject ++ Json.obj(LoggingKeys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined) jsonObject = jsonObject ++ Json.obj(LoggingKeys.ACTOR -> log.actorName.get)

    if (log.componentName.isDefined) jsonObject = jsonObject ++ Json.obj(LoggingKeys.COMPONENT_NAME -> log.componentName.get)

    if (log.ex != NoLogException) jsonObject = jsonObject ++ exceptionJson(log.ex)

    jsonObject = log.id match {
      case RequestId(trackingId, spanId, _) ⇒ jsonObject ++ Json.obj(LoggingKeys.TRACE_ID -> Seq(trackingId, spanId))
      case _                                ⇒ jsonObject
    }

    if (!log.kind.isEmpty) jsonObject = jsonObject ++ Json.obj(LoggingKeys.KIND -> log.kind)

    jsonObject
  }
}
