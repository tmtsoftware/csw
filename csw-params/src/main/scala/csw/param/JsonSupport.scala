package csw.param

import java.time.Instant

import csw.param.Events._
import csw.param.Parameters._
import csw.param.StateVariable._
import csw.param.models.{Choice, Choices, Struct}
import csw.param.parameters._
import spray.json._

object JsonSupport extends JsonSupport

/**
 * Supports conversion of commands and events to/from JSON
 */
//noinspection TypeAnnotation
trait JsonSupport extends DefaultJsonProtocol {

  // JSON formats
  implicit val choiceFormat  = jsonFormat1(Choice.apply)
  implicit val choicesFormat = jsonFormat1(Choices.apply)

  implicit def structFormat: JsonFormat[Struct] = new JsonFormat[Struct] {
    def write(s: Struct): JsValue = JsObject(
      "type"     -> JsString(s.typeName),
      "paramSet" -> s.paramSet.toJson
    )

    def read(json: JsValue): Struct = {
      json match {
        case JsObject(fields) =>
          (fields("type"), fields("name"), fields("paramSet")) match {
            case (JsString(typeName), JsString(name), paramSet) =>
              typeName match {
                case `structType` => Struct(paramSetFormat.read(paramSet))
                case _            => unexpectedJsValueError(json)
              }
            case _ => unexpectedJsValueError(json)
          }
        case _ => unexpectedJsValueError(json)
      }
    }
  }

  implicit def subsystemFormat: JsonFormat[Subsystem] = new JsonFormat[Subsystem] {
    def write(obj: Subsystem) = JsString(obj.name)

    def read(value: JsValue): Subsystem = {
      value match {
        case JsString(subsystemStr) =>
          Subsystem.lookup(subsystemStr) match {
            case Some(subsystem) => subsystem
            case None            => Subsystem.BAD
          }
        // With malformed JSON, return BAD
        case _ => Subsystem.BAD
      }
    }
  }

  implicit def paramSetFormat: JsonFormat[ParameterSet] = new JsonFormat[ParameterSet] {
    def write(parameters: ParameterSet) = JsArray(parameters.map(writeParameter(_)).toList: _*)

    def read(json: JsValue) = json match {
      case a: JsArray => a.elements.map((el: JsValue) => readParameterAndType(el)).toSet
      case _          => unexpectedJsValueError(json)
    }
  }

  implicit def eventTimeFormat: JsonFormat[EventTime] = new JsonFormat[EventTime] {
    def write(et: EventTime): JsValue = JsString(et.toString)

    def read(json: JsValue): EventTime = json match {
      case JsString(s) => Instant.parse(s)
      case _           => unexpectedJsValueError(json)
    }
  }

  implicit val parameterSetKeyFormat = jsonFormat2(Prefix.apply)
  implicit val obsIdFormat           = jsonFormat1(ObsId.apply)
  implicit val eventInfoFormat       = jsonFormat4(EventInfo.apply)

  // config and event type JSON tags
  private val setupType        = classOf[Setup].getSimpleName
  private val observeType      = classOf[Observe].getSimpleName
  private val waitType         = classOf[Wait].getSimpleName
  private val statusEventType  = classOf[StatusEvent].getSimpleName
  private val observeEventType = classOf[ObserveEvent].getSimpleName
  private val systemEventType  = classOf[SystemEvent].getSimpleName
  private val curentStateType  = classOf[CurrentState].getSimpleName
  private val demandStateType  = classOf[DemandState].getSimpleName
  private val structType       = classOf[Struct].getSimpleName

  private def unexpectedJsValueError(x: JsValue) = deserializationError(s"Unexpected JsValue: $x")

  // XXX TODO Use JNumber?
  def writeParameter[S, I /*, J */ ](parameter: Parameter[S /*, J */ ]): JsValue = {
    val result: (JsString, JsValue) = parameter match {
      case i: Parameter[_] => (JsString(i.keyType.entryName), i.toJson)
    }
    JsObject("type" -> result._1, "parameter" -> result._2)
  }

  def readParameterAndType(json: JsValue): Parameter[_ /*, _ */ ] = json match {
    case JsObject(fields) =>
      (fields("type"), fields("parameter")) match {
        case (JsString(name), parameter) => KeyType.withName(name).paramFormat.read(parameter)
        case _                           => unexpectedJsValueError(json)
      }
    case _ => unexpectedJsValueError(json)
  }

  /**
   * Handles conversion of ParameterSetInfo to/from JSON
   */
  implicit def CommandInfoFormat: RootJsonFormat[CommandInfo] = new RootJsonFormat[CommandInfo] {
    override def read(json: JsValue): CommandInfo = json.asJsObject.getFields("obsId", "runId") match {
      case Seq(JsString(obsId), JsString(runId)) =>
        CommandInfo(ObsId(obsId), RunId(runId))
    }

    override def write(obj: CommandInfo): JsValue = JsObject(
      "obsId" -> JsString(obj.obsId.obsId),
      "runId" -> JsString(obj.runId.id)
    )
  }

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
      "info"     -> CommandInfoFormat.write(sequenceCommand.info),
      "prefix"   -> parameterSetKeyFormat.write(sequenceCommand.prefix),
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
      "prefix"   -> parameterSetKeyFormat.write(stateVariable.prefix),
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
              case `curentStateType` => CurrentState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `demandStateType` => DemandState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _                 => unexpectedJsValueError(json)
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
