package csw.messages.commands

import play.api.libs.json._

/**
 * Model representing the name as an identifier of a command
 *
 * @param name represents the name describing command
 */
case class CommandName(name: String)

object CommandName {

  implicit val format: Format[CommandName] = new Format[CommandName] {
    override def writes(obj: CommandName): JsValue           = JsString(obj.name)
    override def reads(json: JsValue): JsResult[CommandName] = JsSuccess(CommandName(json.as[String]))
  }
}
