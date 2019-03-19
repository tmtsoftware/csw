package csw.logging.client.internal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{Behavior, PostStop}
import akka.event.LogSource
import csw.logging.api._
import csw.logging.api.models.LoggingLevels.Level
import csw.logging.api.models.{LoggingLevels, RequestId}
import csw.logging.client.appenders.LogAppender
import csw.logging.client.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.internal.LogActorMessages._
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Promise

/**
 * All log messages are routed to this single Akka Actor. There is one LogActor per logging system.
 * Logging messages from logging API, Java Slf4j and Akka loggers are sent to this actor.
 * Messages are then forwarded to one or more configured appenders.
 */
private[logging] object LogActor {

  def behavior(
      done: Promise[Unit],
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

      Behaviors
        .receiveMessage[LogActorMessages] {
          case log: Log                     => receiveLog(log); Behaviors.same
          case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage); Behaviors.same
          case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J); Behaviors.same
          case logAkka: LogAkka             => receiveLogAkkaMessage(logAkka); Behaviors.same
          case SetLevel(level1)             => level = level1; Behaviors.same
          case SetSlf4jLevel(level1)        => slf4jLogLevel = level1; Behaviors.same
          case SetAkkaLevel(level1)         => akkaLogLevel = level1; Behaviors.same
          case SetAppenders(_appenders)     => appenders = _appenders; Behaviors.same
          case LastAkkaMessage              => akka.event.Logging(ctx.system.toUntyped, this).error("DIE"); Behaviors.same
          case StopLogging                  => Behaviors.stopped
        }
        .receiveSignal {
          case (context, PostStop) =>
            done.success(())
            Behaviors.same
        }
    }
}
