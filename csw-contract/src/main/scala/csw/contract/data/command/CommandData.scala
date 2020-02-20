package csw.contract.data.command

import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.params.commands.CommandIssue._
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.generics.{GChoiceKey, Key, KeyType, Parameter}
import csw.params.core.models.Coords.{CometCoord, Tag}
import csw.params.core.models._
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime

import scala.concurrent.duration.FiniteDuration

trait CommandData {
  val values                              = 100
  val intKey: Key[Int]                    = KeyType.IntKey.make("encoder")
  val structKey: Key[Struct]              = KeyType.StructKey.make("structs")
  val arr1: Array[Int]                    = Array(1, 2, 3, 4, 5)
  val arr2: Array[Int]                    = Array(10, 20, 30, 40, 50)
  val arrayDataKey: Key[ArrayData[Int]]   = KeyType.IntArrayKey.make("filter")
  val matrixDataKey: Key[MatrixData[Int]] = KeyType.IntMatrixKey.make("matrix")
  val utcTimeKey: Key[UTCTime]            = KeyType.UTCTimeKey.make("utcTimeKey")
  val raDecKey: Key[RaDec]                = KeyType.RaDecKey.make("raDecKey")
  val coordsKey: Key[Coords.CometCoord]   = KeyType.CometCoordKey.make("halley's")
  val choice2Key: GChoiceKey = ChoiceKey.make(
    "mode-reset",
    Choices.fromChoices(Choice("c"), Choice("b"), Choice("a"))
  )

  val intParameter: Parameter[Int]                = intKey.set(values)
  val arrayParameter: Parameter[ArrayData[Int]]   = arrayDataKey.set(ArrayData(arr1), ArrayData(arr2))
  val matrixParameter: Parameter[MatrixData[Int]] = matrixDataKey.set(MatrixData.fromArrays(arr1, arr2))
  val structParameter: Parameter[Struct]          = structKey.set(Struct(Set(intParameter, arrayParameter)))
  val coordsParameter: Parameter[Coords.CometCoord] =
    coordsKey.set(CometCoord(Tag("BASE"), 2000.0, Angle(90L), Angle(2L), Angle(100L), 1.4, 0.234))
  val utcTimeParam: Parameter[UTCTime] = utcTimeKey.set(UTCTime(Instant.parse("2017-09-04T16:28:00.123456789Z")))
  val raDecParameter: Parameter[RaDec] = raDecKey.set(RaDec(100, 100))

  val choiceParameter: Parameter[Choice] = choice2Key.set(Array(Choice("c")))

  val paramSet: Set[Parameter[_]] =
    Set(
      intParameter,
      arrayParameter,
      structParameter,
      matrixParameter,
      coordsParameter,
      utcTimeParam,
      raDecParameter,
      choiceParameter
    )

  val prefix                     = new Prefix(Subsystem.CSW, "ncc.trombone")
  val id: Id                     = Id()
  val reason                     = "issue"
  val idleState: StateName       = StateName("idle")
  val currentState: CurrentState = CurrentState(prefix, idleState, paramSet)
  val states: Set[StateName]     = Set(idleState)

  val accepted: Accepted   = Accepted(id)
  val cancelled: Cancelled = Cancelled(id)
  val completed: Completed = Completed(id, Result(paramSet))
  val error: Error         = Error(id, reason)

  val invalid: Invalid = Invalid(id, OtherIssue(reason))
  val locked: Locked   = Locked(id)
  val started: Started = Started(id)

  val obsId: ObsId             = ObsId("obs001")
  val commandName: CommandName = CommandName("move")

  val observe: ControlCommand = Observe(prefix, commandName, Some(obsId))
  val setup: ControlCommand   = Setup(prefix, commandName, Some(obsId))

  val timeout: Timeout = Timeout(FiniteDuration(values, TimeUnit.SECONDS))

  val observeValidate: Validate = Validate(observe)
  val observeSubmit: Submit     = Submit(observe)
  val observeOneway: Oneway     = Oneway(observe)
  val setupQuery: Query         = Query(id)

  val queryFinal: QueryFinal                = QueryFinal(id, timeout)
  val subscribeState: SubscribeCurrentState = SubscribeCurrentState(states)

  val assemblyBusyIssue: CommandIssue                 = AssemblyBusyIssue(reason)
  val idNotAvailableIssue: CommandIssue               = IdNotAvailableIssue(reason)
  val missingKeyIssue: CommandIssue                   = MissingKeyIssue(reason)
  val parameterValueOutOfRangeIssue: CommandIssue     = ParameterValueOutOfRangeIssue(reason)
  val requiredAssemblyUnavailableIssue: CommandIssue  = RequiredAssemblyUnavailableIssue(reason)
  val requiredSequencerUnavailableIssue: CommandIssue = RequiredSequencerUnavailableIssue(reason)
  val requiredServiceUnavailableIssue: CommandIssue   = RequiredServiceUnavailableIssue(reason)
  val requiredHCDUnavailableIssue: CommandIssue       = RequiredHCDUnavailableIssue(reason)
  val hcdBusyIssue: CommandIssue                      = HCDBusyIssue(reason)
  val unresolvedLocationsIssue: CommandIssue          = UnresolvedLocationsIssue(reason)
  val unsupportedCommandInStateIssue: CommandIssue    = UnsupportedCommandInStateIssue(reason)
  val unsupportedCommandIssue: CommandIssue           = UnsupportedCommandIssue(reason)
  val wrongInternalStateIssue: CommandIssue           = WrongInternalStateIssue(reason)
  val wrongNumberOfParametersIssue: CommandIssue      = WrongNumberOfParametersIssue(reason)
  val wrongParameterTypeIssue: CommandIssue           = WrongParameterTypeIssue(reason)
  val wrongPrefixIssue: CommandIssue                  = WrongPrefixIssue(reason)
  val wrongUnitsIssue: CommandIssue                   = WrongUnitsIssue(reason)
  val otherIssue: CommandIssue                        = OtherIssue(reason)

  val result: Result = Result(paramSet)
}
