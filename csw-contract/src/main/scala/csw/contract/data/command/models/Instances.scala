package csw.contract.data.command.models

import csw.location.models.codecs.LocationCodecs
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse.{Accepted, Cancelled, Invalid, Started}
import csw.params.commands._
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

object Instances extends LocationCodecs {
  private val encoder: Key[Int] = KeyType.IntKey.make("encoder")
  private val values            = 100
  private val prefix            = new Prefix(Subsystem.CSW, "someComponent")
  val observe: ControlCommand =
    Observe(prefix, CommandName("command"), Some(ObsId("obsId")))
  val setup: ControlCommand =
    Setup(prefix, CommandName("command"), Some(ObsId("obsId")))
  val runId: Id                    = Id("runId")
  val param: Parameter[Int]        = encoder.set(values)
  private val idleState: StateName = StateName("idle")
  val currentState: CurrentState   = CurrentState(prefix, idleState, Set(param))
  val states: Set[StateName]       = Set(idleState)
  val accepted: CommandResponse    = Accepted(runId)
  val cancelled: CommandResponse   = Cancelled(runId)
  val invalid: CommandResponse     = Invalid(runId, OtherIssue("Internal issue"))
  val started: CommandResponse     = Started(runId)
}
