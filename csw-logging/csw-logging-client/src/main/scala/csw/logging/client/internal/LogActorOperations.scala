package csw.logging.client.internal

import csw.logging.client.internal.JsonExtensions.AnyToJson
import csw.logging.client.internal.LogActorMessages._
import csw.logging.client.scaladsl.RichException
import java.io.{PrintWriter, StringWriter}

import csw.logging.api._
import csw.logging.api.models.RequestId
import csw.logging.client.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import play.api.libs.json.{JsObject, Json}

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
