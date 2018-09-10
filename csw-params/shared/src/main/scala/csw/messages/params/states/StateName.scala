package csw.messages.params.states

import play.api.libs.json.{Json, OFormat}

/**
 * A wrapper class representing the name of a state
 *
 * @param name represents the name of the state for CurrentState or DemandState
 */
case class StateName(name: String)

object StateName {
  private[messages] implicit val format: OFormat[StateName] = Json.format[StateName]
}
