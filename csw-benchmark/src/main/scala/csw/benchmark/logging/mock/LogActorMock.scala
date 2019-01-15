package csw.benchmark.logging.mock

import csw.logging.core.appenders.LogAppender
import csw.logging.core.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.core.internal.LogActorMessages.Log
import csw.logging.core.internal.LoggingLevels
import csw.logging.macros.SourceLocation
import csw.logging.core.scaladsl.noId
import play.api.libs.json.{JsObject, Json}

object LogActorMock {

  val noException = new Exception("No Exception")
  val standardHeaders: JsObject =
    Json.obj(LoggingKeys.HOST -> "hostname", LoggingKeys.NAME -> "test", LoggingKeys.VERSION -> "SNAPSHOT-1.0")

  val sourceLocation = SourceLocation("hcd.scala", "iris", "tromboneHCD", 12)
  val log = Log(
    Some("tromboneHCD"),
    LoggingLevels.DEBUG,
    noId,
    System.currentTimeMillis(),
    Some("testActor"),
    s"See if this is logged",
    Json.obj(),
    sourceLocation,
    new Exception("No Exception")
  )

  def receiveLog(appender: LogAppender): Unit = {
    var jsonObject = Json.obj(
      LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(log.time),
      LoggingKeys.MESSAGE   → log.msg,
      LoggingKeys.SEVERITY  -> log.level.name,
      LoggingKeys.CATEGORY  -> Category.Common.name
    )

    // This lime adds the user map objects as additional JsonObjects if the map is not empty
    jsonObject = jsonObject ++ log.map

    if (!log.sourceLocation.fileName.isEmpty) {
      jsonObject = jsonObject ++ Json.obj(LoggingKeys.FILE -> log.sourceLocation.fileName)
    }

    if (log.sourceLocation.line > 0)
      jsonObject = jsonObject ++ Json.obj(LoggingKeys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ Json.obj(LoggingKeys.CLASS -> c)
      case (p, c)   ⇒ jsonObject ++ Json.obj(LoggingKeys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined)
      jsonObject = jsonObject ++ Json.obj(LoggingKeys.ACTOR -> log.actorName.get)

    if (log.componentName.isDefined)
      jsonObject = jsonObject ++ Json.obj(LoggingKeys.COMPONENT_NAME -> log.componentName.get)

    if (log.ex != noException) jsonObject = jsonObject

    if (!log.kind.isEmpty)
      jsonObject = jsonObject ++ Json.obj(LoggingKeys.KIND -> log.kind)
    appender.append(jsonObject, Category.Common.name)
  }
}
