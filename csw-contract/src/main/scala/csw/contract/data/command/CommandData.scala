package csw.contract.data.command

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.params.commands.CommandIssue._
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.formats.ParamCodecs
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

trait CommandData extends CommandServiceCodecs with ParamCodecs {
  val values                     = 100
  val encoder: Key[Int]          = KeyType.IntKey.make("encoder")
  val prefix                     = new Prefix(Subsystem.CSW, "someComponent")
  val param: Parameter[Int]      = encoder.set(values)
  val id: Id                     = Id()
  val reason                     = "some reason"
  val idleState: StateName       = StateName("idle")
  val currentState: CurrentState = CurrentState(prefix, idleState, Set(param))
  val states: Set[StateName]     = Set(idleState)

  val accepted: CommandResponse  = Accepted(id)
  val cancelled: CommandResponse = Cancelled(id)
  val completed: CommandResponse = Completed(id, Result(param))
  val error: CommandResponse     = Error(id, "Some error")

  val invalid: CommandResponse = Invalid(id, OtherIssue(reason))
  val locked: CommandResponse  = Locked(id)
  val started: CommandResponse = Started(id)

  val obsId: ObsId             = ObsId("obsId")
  val commandName: CommandName = CommandName("move")

  val observe: ControlCommand = Observe(prefix, commandName, Some(obsId))
  val setup: ControlCommand   = Setup(prefix, commandName, Some(obsId))

  val timeout: Timeout = Timeout(FiniteDuration(values, TimeUnit.SECONDS))

  val observeValidate: CommandServiceHttpMessage     = Validate(observe)
  val observeSubmit: CommandServiceHttpMessage       = Submit(observe)
  val observeOneway: CommandServiceHttpMessage       = Oneway(observe)
  val setupQuery: CommandServiceHttpMessage          = Query(id)
  val queryFinal: CommandServiceWebsocketMessage     = QueryFinal(id, timeout)
  val subscribeState: CommandServiceWebsocketMessage = SubscribeCurrentState(states)

  val assemblyBusyIssue: CommandIssue                 = AssemblyBusyIssue(reason)
  val idNotAvailableIssue: CommandIssue               = IdNotAvailableIssue(reason)
  val missingKeyIssue: CommandIssue                   = MissingKeyIssue(reason)
  val parameterValueOutOfRangeIssue: CommandIssue     = ParameterValueOutOfRangeIssue(reason)
  val requiredAssemblyUnavailableIssue: CommandIssue  = RequiredAssemblyUnavailableIssue(reason)
  val requiredSequencerUnavailableIssue: CommandIssue = RequiredSequencerUnavailableIssue(reason)
  val requiredServiceUnavailableIssue: CommandIssue   = RequiredServiceUnavailableIssue(reason)
  val requiredHCDUnavailableIssue: CommandIssue       = RequiredHCDUnavailableIssue(reason)
  val unresolvedLocationsIssue: CommandIssue          = UnresolvedLocationsIssue(reason)
  val unsupportedCommandInStateIssue: CommandIssue    = UnsupportedCommandInStateIssue(reason)
  val unsupportedCommandIssue: CommandIssue           = UnsupportedCommandIssue(reason)
  val wrongInternalStateIssue: CommandIssue           = WrongInternalStateIssue(reason)
  val wrongNumberOfParametersIssue: CommandIssue      = WrongNumberOfParametersIssue(reason)
  val wrongParameterTypeIssue: CommandIssue           = WrongParameterTypeIssue(reason)
  val wrongPrefixIssue: CommandIssue                  = WrongPrefixIssue(reason)
  val wrongUnitsIssue: CommandIssue                   = WrongUnitsIssue(reason)
  val otherIssue: CommandIssue                        = OtherIssue(reason)

  val result: Result = Result(param)
}
