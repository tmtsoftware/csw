package csw.param

import csw.param.Events._
import csw.param.Parameters._
import csw.param.StateVariable._
import csw.param.formats.{EnumJsonSupport, JavaFormats, WrappedArrayProtocol}
import spray.json._

object JsonSupport extends JsonSupport

/**
 * Supports conversion of commands and events to/from JSON
 */
//noinspection TypeAnnotation
trait JsonSupport extends DefaultJsonProtocol with JavaFormats with EnumJsonSupport with WrappedArrayProtocol {

  // JSON formats
  val paramSetFormat    = implicitly[JsonFormat[ParameterSet]]
  val commandInfoFormat = implicitly[JsonFormat[CommandInfo]]
  val prefixFormat      = implicitly[JsonFormat[Prefix]]
  val eventInfoFormat   = implicitly[JsonFormat[EventInfo]]

  // config and event type JSON tags
  private val setupType        = classOf[Setup].getSimpleName
  private val observeType      = classOf[Observe].getSimpleName
  private val waitType         = classOf[Wait].getSimpleName
  private val statusEventType  = classOf[StatusEvent].getSimpleName
  private val observeEventType = classOf[ObserveEvent].getSimpleName
  private val systemEventType  = classOf[SystemEvent].getSimpleName
  private val currentStateType = classOf[CurrentState].getSimpleName
  private val demandStateType  = classOf[DemandState].getSimpleName

  private def unexpectedJsValueError(x: JsValue) = deserializationError(s"Unexpected JsValue: $x")

  /**
   * Writes a SequenceParameterSet to JSON
   *
   * @param sequenceCommand any instance of SequenceCommand
   * @tparam A the type of the command (implied)
   * @return a JsValue object representing the SequenceCommand
   */
  def writeSequenceCommand[A <: SequenceCommand](sequenceCommand: A): JsValue = {
    JsObject(
      "type"     -> JsString(sequenceCommand.typeName),
      "info"     -> commandInfoFormat.write(sequenceCommand.info),
      "prefix"   -> prefixFormat.write(sequenceCommand.prefix),
      "paramSet" -> sequenceCommand.paramSet.toJson
    )
  }

  /**
   * Reads a SequenceCommand back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of the given SequenceCommand type, or an exception if the JSON is not valid for that type
   */
  def readSequenceCommand[A <: SequenceCommand](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("info"), fields("prefix"), fields("paramSet")) match {
          case (JsString(typeName), info, prefix, paramSet) =>
            val commandInfo = info.convertTo[CommandInfo]
            val ck          = prefix.convertTo[Prefix]
            typeName match {
              case `setupType`   => Setup(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `observeType` => Observe(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `waitType`    => Wait(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _             => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  /**
   * Writes a state variable to JSON
   *
   * @param stateVariable any instance of StateVariable
   * @tparam A the type of the StateVariable (implied)
   * @return a JsValue object representing the StateVariable
   */
  def writeStateVariable[A <: StateVariable](stateVariable: A): JsValue = {
    JsObject(
      "type"     -> JsString(stateVariable.typeName),
      "prefix"   -> prefixFormat.write(stateVariable.prefix),
      "paramSet" -> stateVariable.paramSet.toJson
    )
  }

  /**
   * Reads a StateVariable back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of the given StateVariable, or an exception if the JSON is not valid for that type
   */
  def readStateVariable[A <: StateVariable](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("prefix"), fields("paramSet")) match {
          case (JsString(typeName), prefix, paramSet) =>
            val ck = prefix.convertTo[Prefix]
            typeName match {
              case `currentStateType` => CurrentState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `demandStateType`  => DemandState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _                  => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  /**
   * Writes an event to JSON
   *
   * @param event any instance of EventType
   * @tparam A the type of the event (implied)
   * @return a JsValue object representing the event
   */
  def writeEvent[A <: EventType[_]](event: A): JsValue = {
    JsObject(
      "type"     -> JsString(event.typeName),
      "info"     -> eventInfoFormat.write(event.info),
      "paramSet" -> event.paramSet.toJson
    )
  }

  /**
   * Reads an event back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the event (use Any and match on the type if you don't know)
   * @return an instance of the given event type, or an exception if the JSON is not valid for that type
   */
  def readEvent[A <: EventType[_]](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("info"), fields("paramSet")) match {
          case (JsString(typeName), eventInfo, paramSet) =>
            val info = eventInfo.convertTo[EventInfo]
            typeName match {
              case `statusEventType`  => StatusEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `observeEventType` => ObserveEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `systemEventType`  => SystemEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _                  => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }
}
