package csw.messages.params.formats

import csw.messages.commands._
import csw.messages.events._
import csw.messages.params.generics.Parameter
import csw.messages.params.models.{Id, ObsId, Prefix}
import csw.messages.params.states.StateVariable.StateVariable
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import play.api.libs.json._

object JsonSupport extends JsonSupport with DerivedJsonFormats with WrappedArrayProtocol

/**
 * Supports conversion of commands, state variables and events to/from JSON
 */
trait JsonSupport { self: DerivedJsonFormats with WrappedArrayProtocol ⇒

  // JSON formats
  lazy val paramSetFormat: Format[Set[Parameter[_]]] = implicitly[Format[Set[Parameter[_]]]]
  lazy val idFormat: Format[Id]                      = implicitly[Format[Id]]
  lazy val obsIdFormat: Format[Option[ObsId]]        = implicitly[Format[Option[ObsId]]]
  lazy val prefixFormat: Format[Prefix]              = implicitly[Format[Prefix]]
  lazy val commandTypeFormat: Format[CommandName]    = implicitly[Format[CommandName]]
  lazy val eventTimeFormat: Format[EventTime]        = implicitly[Format[EventTime]]
  lazy val eventNameFormat: Format[EventName]        = implicitly[Format[EventName]]
  lazy val stateNameFormat: Format[StateName]        = implicitly[Format[StateName]]

  // config and event type JSON tags
  private val setupType        = classOf[Setup].getSimpleName
  private val observeType      = classOf[Observe].getSimpleName
  private val waitType         = classOf[Wait].getSimpleName
  private val observeEventType = classOf[ObserveEvent].getSimpleName
  private val systemEventType  = classOf[SystemEvent].getSimpleName
  private val currentStateType = classOf[CurrentState].getSimpleName
  private val demandStateType  = classOf[DemandState].getSimpleName

  private def unexpectedJsValueError(x: JsValue) = throw new RuntimeException(s"Unexpected JsValue: $x")

  /**
   * Writes a SequenceParameterSet to JSON
   *
   * @param result any instance of SequenceCommand
   * @tparam A the type of the command (implied)
   * @return a JsValue object representing the SequenceCommand
   */
  def writeSequenceCommand[A <: SequenceCommand](result: A): JsValue = {
    JsObject(
      Seq(
        "type"        → JsString(result.typeName),
        "runId"       → idFormat.writes(result.runId),
        "source"      → prefixFormat.writes(result.source),
        "commandName" → commandTypeFormat.writes(result.commandName),
        "obsId"       → obsIdFormat.writes(result.maybeObsId),
        "paramSet"    → Json.toJson(result.paramSet)
      )
    )
  }

  /**
   * Reads a SequenceCommand back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the command (implied)
   * @return an instance of the given SequenceCommand type, or an exception if the JSON is not valid for that type
   */
  def readSequenceCommand[A <: SequenceCommand](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("runId"), fields("source"), fields("commandName"), fields("obsId"), fields("paramSet")) match {
          case (JsString(typeName), runId, source, commandName, obsId, paramSet) =>
            typeName match {
              case `setupType` =>
                Setup(runId.as[Id],
                      source.as[Prefix],
                      commandName.as[CommandName],
                      obsId.as[Option[ObsId]],
                      paramSet.as[Set[Parameter[_]]]).asInstanceOf[A]
              case `observeType` =>
                Observe(runId.as[Id],
                        source.as[Prefix],
                        commandName.as[CommandName],
                        obsId.as[Option[ObsId]],
                        paramSet.as[Set[Parameter[_]]]).asInstanceOf[A]
              case `waitType` =>
                Wait(runId.as[Id],
                     source.as[Prefix],
                     commandName.as[CommandName],
                     obsId.as[Option[ObsId]],
                     paramSet.as[Set[Parameter[_]]]).asInstanceOf[A]
              case _ => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  implicit val sequenceCommandFormat: Format[SequenceCommand] = new Format[SequenceCommand] {
    override def writes(o: SequenceCommand): JsValue             = writeSequenceCommand(o)
    override def reads(json: JsValue): JsResult[SequenceCommand] = readSequenceCommand(json)
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
      Seq(
        "type"      → JsString(stateVariable.typeName),
        "prefix"    → prefixFormat.writes(stateVariable.prefix),
        "stateName" → stateNameFormat.writes(stateVariable.stateName),
        "paramSet"  → Json.toJson(stateVariable.paramSet)
      )
    )
  }

  /**
   * Reads a StateVariable back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the StateVariable (implied)
   * @return an instance of the given StateVariable, or an exception if the JSON is not valid for that type
   */
  def readStateVariable[A <: StateVariable](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("prefix"), fields("stateName"), fields("paramSet")) match {
          case (JsString(typeName), prefix, stateName, paramSet) =>
            val ck    = prefix.as[Prefix]
            val sName = stateName.as[StateName]
            typeName match {
              case `currentStateType` => CurrentState(ck, sName, paramSetFormat.reads(paramSet).get).asInstanceOf[A]
              case `demandStateType`  => DemandState(ck, sName, paramSetFormat.reads(paramSet).get).asInstanceOf[A]
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
  def writeEvent[A <: Event](event: A): JsValue = {
    JsObject(
      Seq(
        "type"      → JsString(event.paramType.typeName),
        "eventId"   → idFormat.writes(event.eventId),
        "source"    → prefixFormat.writes(event.source),
        "eventName" → eventNameFormat.writes(event.eventName),
        "eventTime" → eventTimeFormat.writes(event.eventTime),
        "paramSet"  → Json.toJson(event.paramSet)
      )
    )
  }

  /**
   * Reads an event back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the event (use Any and match on the type if you don't know)
   * @return an instance of the given event type, or an exception if the JSON is not valid for that type
   */
  def readEvent[A <: Event](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("eventId"), fields("source"), fields("eventName"), fields("eventTime"), fields("paramSet")) match {
          case (JsString(typeName), eventId, source, name, eventTime, paramSet) =>
            typeName match {
              case `observeEventType` =>
                ObserveEvent(
                  eventId.as[Id],
                  source.as[Prefix],
                  name.as[EventName],
                  eventTime.as[EventTime],
                  paramSet.as[Set[Parameter[_]]]
                ).asInstanceOf[A]
              case `systemEventType` =>
                SystemEvent(
                  eventId.as[Id],
                  source.as[Prefix],
                  name.as[EventName],
                  eventTime.as[EventTime],
                  paramSet.as[Set[Parameter[_]]]
                ).asInstanceOf[A]
              case _ => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  /**
   * Writes a Result to JSON
   *
   * @param result any instance of Result
   * @return a JsValue object representing the Result
   */
  def writeResult(result: Result): JsValue = {
    JsObject(
      Seq(
        "prefix"   -> prefixFormat.writes(result.prefix),
        "paramSet" -> Json.toJson(result.paramSet)
      )
    )
  }

  /**
   * Reads a Result back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of Result, or an exception if the JSON is not valid for that type
   */
  def readResult(json: JsValue): Result = {
    json match {
      case JsObject(fields) =>
        (fields("prefix"), fields("paramSet")) match {
          case (prefix, paramSet) =>
            Result(prefix.as[Prefix], paramSetFormat.reads(paramSet).get)
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }
}
