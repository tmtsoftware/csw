/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.formats

import java.nio.charset.StandardCharsets
import csw.params.commands.*
import csw.params.core.generics.Parameter
import csw.params.core.models.Coords.{AltAzCoord, CometCoord, EqCoord, EqFrame, MinorPlanetCoord, SolarSystemCoord}
import csw.params.core.models.ProperMotion
import csw.params.core.states.StateVariable
import csw.params.events.*
import io.bullet.borer.{Decoder, Encoder, Json}
import play.api.libs.json.{Json as PJson, *}

object JsonSupport extends JsonSupport

// This is needed for Java support, since it is a top level object instead of a trait, making it
// possible to call methods like writeSequenceCommand() on it from Java.
object JavaJsonSupport extends JsonSupport

/**
 * Supports conversion of commands, state variables and events to/from JSON
 */
trait JsonSupport {

  import ParamCodecs._

  def writes[T: Encoder](x: T): JsValue = PJson.parse(Json.encode(x).toUtf8String)
  def reads[T: Decoder](x: JsValue): T  = Json.decode(x.toString().getBytes(StandardCharsets.UTF_8)).to[T].value

  /**
   * Writes a SequenceParameterSet to JSON
   *
   * @param result any instance of SequenceCommand
   * @return a JsValue object representing the SequenceCommand
   */
  def writeSequenceCommand(result: SequenceCommand): JsValue = writes(result)

  /**
   * Reads a SequenceCommand back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the command (implied)
   * @return an instance of the given SequenceCommand type, or an exception if the JSON is not valid for that type
   */
  def readSequenceCommand[A <: SequenceCommand](json: JsValue): A = reads[SequenceCommand](json).asInstanceOf[A]

  /**
   * Writes a state variable to JSON
   *
   * @param stateVariable any instance of StateVariable
   * @return a JsValue object representing the StateVariable
   */
  def writeStateVariable(stateVariable: StateVariable): JsValue = writes(stateVariable)

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
   * @return a JsValue object representing the event
   */
  def writeEvent(event: Event): JsValue = writes(event)

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

  // --- The defs below were added to support calls from Java ---
  // --- (since Scala 3, calling the borer inline/macro code from Java doesn't work) ---

  def writeProperMotion(pm: ProperMotion): JsValue  = JsonSupport.writes(pm)
  def readProperMotion(json: JsValue): ProperMotion = reads[ProperMotion](json)

  def writeEqFrame(pm: EqFrame): JsValue  = JsonSupport.writes(pm)
  def readEqFrame(json: JsValue): EqFrame = reads[EqFrame](json)

  def writeAltAzCoord(pm: AltAzCoord): JsValue  = JsonSupport.writes(pm)
  def readAltAzCoord(json: JsValue): AltAzCoord = reads[AltAzCoord](json)

  def writeSolarSystemCoord(pm: SolarSystemCoord): JsValue  = JsonSupport.writes(pm)
  def readSolarSystemCoord(json: JsValue): SolarSystemCoord = reads[SolarSystemCoord](json)

  def writeMinorPlanetCoord(pm: MinorPlanetCoord): JsValue  = JsonSupport.writes(pm)
  def readMinorPlanetCoord(json: JsValue): MinorPlanetCoord = reads[MinorPlanetCoord](json)

  def writeCometCoord(pm: CometCoord): JsValue  = JsonSupport.writes(pm)
  def readCometCoord(json: JsValue): CometCoord = reads[CometCoord](json)

  def writeEqCoord(pm: EqCoord): JsValue  = JsonSupport.writes(pm)
  def readEqCoord(json: JsValue): EqCoord = reads[EqCoord](json)
}
