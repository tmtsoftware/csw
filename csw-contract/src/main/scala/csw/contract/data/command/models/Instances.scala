package csw.contract.data.command.models
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.{DemandMatcher, DemandMatcherAll, PresenceMatcher, StateMatcher}
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.ModelAdt
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.KeyType
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

object Instances extends CommandServiceCodecs {
  private val encoder = KeyType.IntKey.make("encoder")
  private val values  = 100
  private val prefix  = new Prefix(Subsystem.CSW, "someComponent")
  private val param   = encoder.set(values)
  val id              = Id("runId")

  private val idleState: StateName = StateName("idle")
  val currentState: CurrentState   = CurrentState(prefix, idleState, Set(param))
  val states: Set[StateName]       = Set(idleState)

  val accepted: CommandResponse  = Accepted(id)
  val cancelled: CommandResponse = Cancelled(id)
  val completed: CommandResponse = Completed(id, Result(param))
  val error: CommandResponse     = Error(id, "Some error")
  val invalid: CommandResponse   = Invalid(id, OtherIssue("Internal issue"))
  val locked: CommandResponse    = Locked(id)
  val started: CommandResponse   = Started(id)

  val observe: ControlCommand =
    Observe(prefix, CommandName("command"), Some(ObsId("obsId")))
  val setup: ControlCommand =
    Setup(prefix, CommandName("command"), Some(ObsId("obsId")))

  val demandMatcher: StateMatcher =
    DemandMatcher(DemandState(prefix, idleState, Set(param)), withUnits = true, Timeout(FiniteDuration(values, TimeUnit.SECONDS)))
  val demandMatcherAll: StateMatcher =
    DemandMatcherAll(DemandState(prefix, idleState, Set(param)), Timeout(FiniteDuration(values, TimeUnit.SECONDS)))
  val presenceMatcher: StateMatcher =
    PresenceMatcher(prefix, idleState, Timeout(FiniteDuration(values, TimeUnit.SECONDS)))

  val models: Map[String, ModelAdt] = Map(
    "ControlCommand" -> ModelAdt(
      List(observe, setup)
    ),
    "Id" -> ModelAdt(
      List(id)
    ),
    "StateName" -> ModelAdt(
      List(idleState)
    ),
    "SubmitResponse" -> ModelAdt(
      List(cancelled, completed, error, invalid, locked, started)
    ),
    "OneWayResponse" -> ModelAdt(
      List(accepted, invalid, locked)
    ),
    "ValidateResponse" -> ModelAdt(
      List(accepted, invalid, locked)
    ),
    "CurrentState" -> ModelAdt(
      List(currentState)
    )
  )
}
