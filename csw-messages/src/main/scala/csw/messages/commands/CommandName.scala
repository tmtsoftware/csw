package csw.messages.commands

import play.api.libs.json.{Json, OFormat}

/**
 * Model representing the name as an identifier of a command
 *
 * @param name represents the name describing command
 */
case class CommandName(name: String)

object CommandName {
  implicit val format: OFormat[CommandName] = Json.format[CommandName]
}
