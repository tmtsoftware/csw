/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import csw.logging.client.appenders.LogAppender
import csw.logging.macros.SourceLocation
import csw.logging.models.{AnyId, Level}
import csw.prefix.models.Prefix
import play.api.libs.json.JsObject

// Parent trait for Log messages shared with Log Actor
private[csw] sealed trait LogActorMessages

private[csw] object LogActorMessages {

  // Model for common Log messages shared with Log Actor
  case class Log(
      prefix: Option[Prefix],
      level: Level,
      id: AnyId,
      time: Long,
      actorName: Option[String],
      msg: String,
      map: JsObject,
      sourceLocation: SourceLocation,
      ex: Throwable,
      kind: String = ""
  ) extends LogActorMessages

  case class SetLevel(level: Level) extends LogActorMessages

  // Model for Log messages to be shared with Log Actor which are logged using 'alternative' method of logger
  case class LogAltMessage(category: String, time: Long, jsonObject: JsObject, id: AnyId, ex: Throwable) extends LogActorMessages

  case class LogSlf4j(level: Level, time: Long, className: String, msg: String, line: Int, file: String, ex: Throwable)
      extends LogActorMessages

  case class SetSlf4jLevel(level: Level) extends LogActorMessages

  case class LogPekko(time: Long, level: Level, source: String, clazz: Class[?], msg: Any, cause: Option[Throwable])
      extends LogActorMessages

  case class SetPekkoLevel(level: Level) extends LogActorMessages

  case class SetAppenders(appenders: List[LogAppender]) extends LogActorMessages

  case object LastPekkoMessage extends LogActorMessages

  case object StopLogging extends LogActorMessages

}
