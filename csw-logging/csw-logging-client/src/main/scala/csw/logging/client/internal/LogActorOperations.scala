/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import java.io.{PrintWriter, StringWriter}

import csw.logging.api._
import csw.logging.client.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.internal.JsonExtensions.AnyToJson
import csw.logging.client.internal.LogActorMessages._
import csw.logging.client.scaladsl.RichException
import csw.logging.models.{Level, RequestId}
import play.api.libs.json.{JsObject, Json}

private[logging] object LogActorOperations {
  def exToJson(ex: Throwable): JsObject = {
    val name = ex.getClass.toString
    ex match {
      case ex: RichException => Json.obj(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.richMsg.asJson)
      case ex: Throwable     => Json.obj(LoggingKeys.EX -> name, LoggingKeys.MESSAGE -> ex.getMessage)
    }
  }

  def generateAltMessageJson(logAltMessage: LogAltMessage): JsObject = {
    var jsonObject = logAltMessage.jsonObject
    if (logAltMessage.ex != NoLogException) jsonObject = jsonObject ++ exceptionJson(logAltMessage.ex)

    jsonObject = logAltMessage.id match {
      case RequestId(trackingId, spanId, _) => jsonObject ++ Json.obj(LoggingKeys.TRACE_ID -> Seq(trackingId, spanId))
      case _                                => jsonObject
    }

    jsonObject ++ Json.obj(LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(logAltMessage.time))
  }

  def generateLogSlf4jJson(logSlf4j: LogSlf4j, slf4jLogLevel: Level): Option[JsObject] =
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

      Some(jsonObject)
    }
    else None

  def generateLogAkkaJson(logAkka: LogAkka, akkaLogLevel: Level): Option[JsObject] =
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
      Some(jsonObject)
    }
    else None

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
    stack.toList
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
      LoggingKeys.PLAINSTACK -> extractPlainStacktrace(ex)
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
      LoggingKeys.MESSAGE   -> log.msg,
      LoggingKeys.SEVERITY  -> log.level.name,
      LoggingKeys.CATEGORY  -> Category.Common.name
    )

    // This lime adds the user map objects as additional JsonObjects if the map is not empty
    jsonObject = jsonObject ++ log.map

    if (!log.sourceLocation.fileName.isEmpty) jsonObject = jsonObject ++ Json.obj(LoggingKeys.FILE -> log.sourceLocation.fileName)

    if (log.sourceLocation.line > 0) jsonObject = jsonObject ++ Json.obj(LoggingKeys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") => jsonObject
      case ("", c)  => jsonObject ++ Json.obj(LoggingKeys.CLASS -> c)
      case (p, c)   => jsonObject ++ Json.obj(LoggingKeys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined) jsonObject = jsonObject ++ Json.obj(LoggingKeys.ACTOR -> log.actorName.get)

    if (log.prefix.isDefined)
      jsonObject = jsonObject ++
        Json.obj(LoggingKeys.PREFIX -> log.prefix.get.toString) ++
        Json.obj(LoggingKeys.SUBSYSTEM -> log.prefix.get.subsystem.name) ++
        Json.obj(LoggingKeys.COMPONENT_NAME -> log.prefix.get.componentName)

    if (log.ex != NoLogException) jsonObject = jsonObject ++ exceptionJson(log.ex)

    jsonObject = log.id match {
      case RequestId(trackingId, spanId, _) => jsonObject ++ Json.obj(LoggingKeys.TRACE_ID -> Seq(trackingId, spanId))
      case _                                => jsonObject
    }

    if (!log.kind.isEmpty) jsonObject = jsonObject ++ Json.obj(LoggingKeys.KIND -> log.kind)

    jsonObject
  }
}
