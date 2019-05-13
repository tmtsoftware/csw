package csw.params.core.formats

import csw.params.commands._
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.params.testdata.ParamSetData
import org.scalatest.{FunSuite, Matchers}

class CborTest extends FunSuite with Matchers {

  private val prefix      = Prefix("wfos.blue.filter")
  private val eventName   = EventName("filter wheel")
  private val commandName = CommandName("filter wheel")
  private val maybeObsId  = Some(ObsId("obsId"))

  test("should encode and decode event with paramSet having all key-types") {
    val event       = ObserveEvent(prefix, eventName, ParamSetData.paramSet)
    val bytes       = EventCbor.encode(event)
    val parsedEvent = EventCbor.decode[ObserveEvent](bytes)
    parsedEvent shouldEqual event
  }

  test("should encode and decode system event") {
    val systemEvent = SystemEvent(prefix, eventName)
    val bytes       = EventCbor.encode(systemEvent)
    val parsedEvent = EventCbor.decode[SystemEvent](bytes)
    parsedEvent shouldEqual systemEvent
  }

  test("should encode and decode observe event") {
    val observeEvent = ObserveEvent(prefix, eventName)
    val bytes        = EventCbor.encode(observeEvent)
    val parsedEvent  = EventCbor.decode[ObserveEvent](bytes)
    parsedEvent shouldEqual observeEvent
  }

  test("should encode base-type event and decode concrete-type") {
    val event: Event = ObserveEvent(prefix, eventName)
    val bytes        = EventCbor.encode(event)
    val parsedEvent  = EventCbor.decode[ObserveEvent](bytes)
    parsedEvent shouldEqual event
  }

  test("should encode and decode a command with paramSet having all key-types") {
    val command       = Setup(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes         = CommandCbor.encode(command)
    val parsedCommand = CommandCbor.decode[Setup](bytes)
    parsedCommand shouldEqual command
  }

  test("should encode and decode a setup command") {
    val setupCommand  = Setup(prefix, commandName, maybeObsId)
    val bytes         = CommandCbor.encode(setupCommand)
    val parsedCommand = CommandCbor.decode[Setup](bytes)
    parsedCommand shouldEqual setupCommand
  }

  test("should encode and decode an observe command") {
    val observeCommand = Observe(prefix, commandName, maybeObsId)
    val bytes          = CommandCbor.encode(observeCommand)
    val parsedCommand  = CommandCbor.decode[Observe](bytes)
    parsedCommand shouldEqual observeCommand
  }

  test("should encode and decode a wait command") {
    val waitCommand   = Wait(prefix, commandName, maybeObsId)
    val bytes         = CommandCbor.encode(waitCommand)
    val parsedCommand = CommandCbor.decode[Wait](bytes)
    parsedCommand shouldEqual waitCommand
  }

  test("should encode base-type command and decode a concrete-type") {
    val command: Command = Setup(prefix, commandName, maybeObsId)
    val bytes            = CommandCbor.encode(command)
    val parsedCommand    = CommandCbor.decode[Setup](bytes)
    parsedCommand shouldEqual command
  }

  test("should encode concrete-type command and decode base-type") {
    val command       = Setup(prefix, commandName, maybeObsId)
    val bytes         = CommandCbor.encode(command)
    val parsedCommand = CommandCbor.decode[Command](bytes)
    parsedCommand shouldEqual command
  }
}
