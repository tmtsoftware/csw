/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter.TypedActorSystemOps
import org.apache.pekko.actor.typed.{Behavior, PostStop}
import org.apache.pekko.event.LogSource
import csw.logging.client.appenders.LogAppender
import csw.logging.client.commons.Category
import csw.logging.client.internal.LogActorMessages.*
import csw.logging.models.Level
import play.api.libs.json.JsObject

import scala.concurrent.Promise

/**
 * All log messages are routed to this single Pekko Actor. There is one LogActor per logging system.
 * Logging messages from logging API, Java Slf4j and Pekko loggers are sent to this actor.
 * Messages are then forwarded to one or more configured appenders.
 */
private[logging] object LogActor {

  def behavior(
      done: Promise[Unit],
      appends: Seq[LogAppender],
      initLevel: Level,
      initSlf4jLevel: Level,
      initPekkoLevel: Level
  ): Behavior[LogActorMessages] =
    Behaviors.setup { ctx =>
      import LogActorOperations._

      implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
        def genString(o: AnyRef): String           = o.getClass.getName
        override def getClazz(o: AnyRef): Class[_] = o.getClass
      }

      var level: Level                = initLevel
      var pekkoLogLevel: Level        = initPekkoLevel
      var slf4jLogLevel: Level        = initSlf4jLevel
      var appenders: Seq[LogAppender] = appends

      // Send JSON log object for each appender configured for the logging system
      def append(baseMsg: JsObject, category: String): Unit =
        for (appender <- appenders) appender.append(baseMsg, category)

      def receiveAltMessage(logAltMessage: LogAltMessage): Unit = {
        val jsonObject = generateAltMessageJson(logAltMessage)
        append(jsonObject, logAltMessage.category)
      }

      def receiveLogSlf4j(logSlf4j: LogSlf4j): Unit =
        if (logSlf4j.level >= LoggingState.slf4jLogLevel) {
          val jsonObject = generateLogSlf4jJson(logSlf4j, slf4jLogLevel)
          jsonObject.foreach(json => append(json, Category.Common.name))
        }

      def receiveLogPekkoMessage(logPekko: LogPekko): Unit =
        if (logPekko.level >= LoggingState.pekkoLogLevel) {
          val jsonObject = generateLogPekkoJson(logPekko, pekkoLogLevel)
          jsonObject.foreach(json => append(json, Category.Common.name))
        }

      def receiveLog(log: Log): Unit = {
        if (log.level >= LoggingState.logLevel)
          append(createJsonFromLog(log), Category.Common.name)
      }

      Behaviors
        .receiveMessage[LogActorMessages] {
          case log: Log                     => receiveLog(log); Behaviors.same
          case logAltMessage: LogAltMessage => receiveAltMessage(logAltMessage); Behaviors.same
          case logSlf4J: LogSlf4j           => receiveLogSlf4j(logSlf4J); Behaviors.same
          case logPekko: LogPekko           => receiveLogPekkoMessage(logPekko); Behaviors.same
          case SetLevel(level1)             => level = level1; Behaviors.same
          case SetSlf4jLevel(level1)        => slf4jLogLevel = level1; Behaviors.same
          case SetPekkoLevel(level1)        => pekkoLogLevel = level1; Behaviors.same
          case SetAppenders(_appenders)     => appenders = _appenders; Behaviors.same
          case LastPekkoMessage => org.apache.pekko.event.Logging(ctx.system.toClassic, this).error("DIE"); Behaviors.same
          case StopLogging      => Behaviors.stopped
        }
        .receiveSignal { case (_, PostStop) =>
          done.success(())
          Behaviors.same
        }
    }
}
