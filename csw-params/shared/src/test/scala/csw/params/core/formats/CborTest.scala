package csw.params.core.formats

import csw.params.commands.CommandIssue._
import csw.params.commands._
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.params.testdata.ParamSetData
import csw.prefix.models.Prefix
import io.bullet.borer.Cbor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CborTest extends AnyFunSuite with Matchers {

  private val prefix      = Prefix("wfos.blue.filter")
  private val eventName   = EventName("filter wheel")
  private val commandName = CommandName("filter wheel")
  private val maybeObsId  = Some(ObsId("2020A-001-123"))

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
    val command       = Setup(prefix, commandName, maybeObsId)
    val bytes         = CommandCbor.encode(command)
    val parsedCommand = CommandCbor.decode[Setup](bytes)
    parsedCommand shouldEqual command
  }

  test("should encode concrete-type command and decode base-type") {
    val command       = Setup(prefix, commandName, maybeObsId)
    val bytes         = CommandCbor.encode(command)
    val parsedCommand = CommandCbor.decode[ControlCommand](bytes)
    parsedCommand shouldEqual command
  }

  import org.scalatest.prop.TableDrivenPropertyChecks.forAll
  import org.scalatest.prop.Tables.Table

  test("should encode concrete-type CommandIssue and decode base-type | CSW-92") {
    val testData = Table(
      "CommandIssue models",
      MissingKeyIssue(""),
      WrongPrefixIssue(""),
      WrongParameterTypeIssue(""),
      WrongUnitsIssue(""),
      WrongNumberOfParametersIssue(""),
      AssemblyBusyIssue(""),
      UnresolvedLocationsIssue(""),
      ParameterValueOutOfRangeIssue(""),
      WrongInternalStateIssue(""),
      UnsupportedCommandIssue(""),
      UnsupportedCommandInStateIssue(""),
      RequiredServiceUnavailableIssue(""),
      RequiredHCDUnavailableIssue(""),
      HCDBusyIssue(""),
      RequiredAssemblyUnavailableIssue(""),
      RequiredSequencerUnavailableIssue(""),
      WrongCommandTypeIssue(""),
      OtherIssue("")
    )

    forAll(testData) { commandIssue =>
      val bytes = CommandIssueCbor.encode(commandIssue)
      CommandIssueCbor.decode[CommandIssue](bytes) shouldEqual commandIssue
    }
  }

  test("should encode and decode Result") {
    import ParamCodecs.resultCodec
    val result = Result(ParamSetData.paramSet)
    val bytes  = Cbor.encode[Result](result).toByteArray
    Cbor.decode(bytes).to[Result].value shouldEqual result
  }
}
