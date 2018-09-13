package csw.command.models.framework

import acyclic.skipped
import akka.actor.typed.ActorRef
import csw.command.messages.ComponentMessage
import csw.messages.TMTSerializable
import csw.messages.commands.Nameable
import csw.messages.params.states.StateName

/**
 * LifecycleStateChanged represents a notification of state change in a component
 *
 * @param publisher the reference of component's supervisor for which the state changed
 * @param state the new state the component went into
 */
case class LifecycleStateChanged private[framework] (publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState)
    extends TMTSerializable

object LifecycleStateChanged {
  implicit object NameableLifecycleStateChanged extends Nameable[LifecycleStateChanged] {
    override def name(state: LifecycleStateChanged): StateName = StateName(state.state.toString)
  }
}
