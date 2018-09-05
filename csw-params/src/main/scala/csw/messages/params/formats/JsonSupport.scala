package csw.messages.params.formats

import csw.messages.commands._
import csw.messages.events._
import csw.messages.params.states.StateVariable.StateVariable
import julienrf.json.derived
import play.api.libs.json._

object JsonSupport extends JsonSupport with DerivedJsonFormats

//TODO: Why is Java support required? Please delete this and corrosponding tests once confirmed
object JavaJsonSupport extends JsonSupport with DerivedJsonFormats

/**
 * Supports conversion of commands, state variables and events to/from JSON
 */
trait JsonSupport { self: DerivedJsonFormats ⇒

  def format[T](implicit x: Format[T]): Format[T] = x
  def writes[T: Writes](x: T): JsValue            = Json.toJson(x)
  def reads[T: Reads](x: JsValue): T              = x.as[T]

  implicit val commandFormat: OFormat[Command]             = derived.flat.oformat((__ \ "type").format[String])
  implicit val stateVariableFormat: OFormat[StateVariable] = derived.flat.oformat((__ \ "type").format[String])
  implicit val eventFormat: OFormat[Event]                 = derived.flat.oformat((__ \ "type").format[String])

  implicit val sequenceCommandFormat: Reads[SequenceCommand] = {
    commandFormat.collect(JsonValidationError("invalid sequence command")) {
      case x: SequenceCommand ⇒ x
    }
  }

  implicit val controlCommandFormat: Reads[ControlCommand] = {
    commandFormat.collect(JsonValidationError("invalid control command")) {
      case x: ControlCommand ⇒ x
    }
  }

  /**
   * Writes a SequenceParameterSet to JSON
   *
   * @param result any instance of SequenceCommand
   * @tparam A the type of the command (implied)
   * @return a JsValue object representing the SequenceCommand
   */
  def writeSequenceCommand[A <: Command](result: A): JsValue = writes(result)

  /**
   * Reads a SequenceCommand back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the command (implied)
   * @return an instance of the given SequenceCommand type, or an exception if the JSON is not valid for that type
   */
  def readSequenceCommand[A <: Command](json: JsValue): A = reads[Command](json).asInstanceOf[A]

  /**
   * Writes a state variable to JSON
   *
   * @param stateVariable any instance of StateVariable
   * @tparam A the type of the StateVariable (implied)
   * @return a JsValue object representing the StateVariable
   */
  def writeStateVariable[A <: StateVariable](stateVariable: A): JsValue = writes(stateVariable)

  /**
   * Reads a StateVariable back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the StateVariable (implied)
   * @return an instance of the given StateVariable, or an exception if the JSON is not valid for that type
   */
  def readStateVariable[A <: StateVariable](json: JsValue): A = reads[StateVariable](json).asInstanceOf[A]

  /**
   * Writes an event to JSON
   *
   * @param event any instance of EventType
   * @tparam A the type of the event (implied)
   * @return a JsValue object representing the event
   */
  def writeEvent[A <: Event](event: A): JsValue = writes(event)

  /**
   * Reads an event back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the event (use Any and match on the type if you don't know)
   * @return an instance of the given event type, or an exception if the JSON is not valid for that type
   */
  def readEvent[A <: Event](json: JsValue): A = reads[Event](json).asInstanceOf[A]

  /**
   * Writes a Result to JSON
   *
   * @param result any instance of Result
   * @return a JsValue object representing the Result
   */
  def writeResult(result: Result): JsValue = writes(result)

  /**
   * Reads a Result back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of Result, or an exception if the JSON is not valid for that type
   */
  def readResult(json: JsValue): Result = reads[Result](json)
}
