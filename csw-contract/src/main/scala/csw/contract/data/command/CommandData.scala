/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data.command

import java.util.concurrent.TimeUnit

import org.apache.pekko.util.Timeout
import csw.command.api.messages.CommandServiceRequest.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceStreamRequest.{QueryFinal, SubscribeCurrentState}
import csw.params.commands.CommandIssue.*
import csw.params.commands.CommandResponse.*
import csw.params.commands.*
import csw.params.core.generics.Parameter
import csw.params.core.models.*
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

trait CommandData {

  val values = 100

  val paramSet: Set[Parameter[?]] = ParamSetData.paramSet

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

  val obsId: ObsId             = ObsId("2020A-001-123")
  val commandName: CommandName = CommandName("move")

  val observe: ControlCommand             = Observe(prefix, commandName, Some(obsId))
  val observeWithoutObsId: ControlCommand = Observe(prefix, commandName)
  val setup: ControlCommand               = Setup(prefix, commandName, Some(obsId))
  val setupWithoutObsId: ControlCommand   = Setup(prefix, commandName)

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
  val wrongCommandTypeIssue: CommandIssue             = WrongCommandTypeIssue(reason)
  val otherIssue: CommandIssue                        = OtherIssue(reason)

  val result: Result = Result(paramSet)
}
