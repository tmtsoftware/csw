package csw.messages.ccs.commands

import play.api.libs.json.{Json, OFormat}

case class CommandName(name: String)

object CommandName {
  implicit val format: OFormat[CommandName] = Json.format[CommandName]
}
