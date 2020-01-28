package csw.contract.data.command

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.command.api.{DemandMatcher, DemandMatcherAll, PresenceMatcher, StateMatcher}
import csw.contract.generator.models.ClassNameHelpers.name
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.{Endpoint, ModelAdt}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

object Models extends CommandServiceCodecs {
  val values                = 100
  val encoder: Key[Int]     = KeyType.IntKey.make("encoder")
  val prefix                = new Prefix(Subsystem.CSW, "someComponent")
  val param: Parameter[Int] = encoder.set(values)
  val id: Id                = Id("runId")

  val idleState: StateName       = StateName("idle")
  val currentState: CurrentState = CurrentState(prefix, idleState, Set(param))
  val states: Set[StateName]     = Set(idleState)

  val accepted: CommandResponse  = Accepted(id)
  val cancelled: CommandResponse = Cancelled(id)
  val completed: CommandResponse = Completed(id, Result(param))
  val error: CommandResponse     = Error(id, "Some error")
  val invalid: CommandResponse   = Invalid(id, OtherIssue("Internal issue"))
  val locked: CommandResponse    = Locked(id)
  val started: CommandResponse   = Started(id)

  val observe: ControlCommand = Observe(prefix, CommandName("command"), Some(ObsId("obsId")))
  val setup: ControlCommand   = Setup(prefix, CommandName("command"), Some(ObsId("obsId")))

  val timeout: Timeout               = Timeout(FiniteDuration(values, TimeUnit.SECONDS))
  val state: DemandState             = DemandState(prefix, idleState, Set(param))
  val demandMatcher: StateMatcher    = DemandMatcher(state, withUnits = true, timeout)
  val demandMatcherAll: StateMatcher = DemandMatcherAll(state, timeout)
  val presenceMatcher: StateMatcher  = PresenceMatcher(prefix, idleState, timeout)

  val observeValidate: CommandServiceHttpMessage     = Validate(observe)
  val setupValidate: CommandServiceHttpMessage       = Validate(setup)
  val observeSubmit: CommandServiceHttpMessage       = Submit(observe)
  val setupSubmit: CommandServiceHttpMessage         = Submit(setup)
  val observeOneway: CommandServiceHttpMessage       = Oneway(observe)
  val setupOneway: CommandServiceHttpMessage         = Oneway(setup)
  val setupQuery: CommandServiceHttpMessage          = Query(id)
  val queryFinal: CommandServiceWebsocketMessage     = QueryFinal(id, timeout)
  val subscribeState: CommandServiceWebsocketMessage = SubscribeCurrentState(states)

  val models: Map[String, ModelAdt] = Map(
    name[ControlCommand]   -> ModelAdt(observe, setup),
    name[Id]               -> ModelAdt(id),
    name[StateName]        -> ModelAdt(idleState),
    name[SubmitResponse]   -> ModelAdt(cancelled, completed, error, invalid, locked, started),
    name[OnewayResponse]   -> ModelAdt(accepted, invalid, locked),
    name[ValidateResponse] -> ModelAdt(accepted, invalid, locked),
    name[CurrentState]     -> ModelAdt(currentState),
    "Requests" -> ModelAdt(
      observeValidate,
      setupValidate,
      observeSubmit,
      setupSubmit,
      observeOneway,
      setupOneway,
      setupQuery,
      queryFinal,
      subscribeState
    )
  )

  val httpEndpoints = List(
    Endpoint(name[Validate], name[ValidateResponse], List.empty),
    Endpoint(name[Submit], name[SubmitResponse], List.empty),
    Endpoint(name[Query], name[SubmitResponse], List.empty),
    Endpoint(name[Oneway], name[OnewayResponse], List.empty)
  )
  val webSocketsEndpoints = List(
    Endpoint(name[QueryFinal], name[SubmitResponse], List.empty),
    Endpoint(name[SubscribeCurrentState], name[CurrentState], List.empty)
  )
}
