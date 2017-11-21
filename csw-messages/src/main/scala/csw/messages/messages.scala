package csw.messages

import akka.typed.ActorRef
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.TrackingEvent
import csw.messages.models.PubSub.SubscriberMessage
import csw.messages.models._
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState

sealed trait ComponentMessage

sealed trait CommonMessage extends ComponentMessage
object CommonMessage {
  case class UnderlyingHookFailed(throwable: Throwable)          extends CommonMessage
  case class TrackingEventReceived(trackingEvent: TrackingEvent) extends CommonMessage
}

sealed trait IdleMessage extends ComponentMessage
object IdleMessage {
  case object Initialize extends IdleMessage
}

sealed trait CommandMessage extends RunningMessage with SupervisorLockMessage {
  def replyTo: ActorRef[CommandResponse]
  def command: ControlCommand
}

object CommandMessage {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
}

sealed trait SupervisorLockMessage extends SupervisorRunningMessage
object SupervisorLockMessage {
  case class Lock(prefix: String, token: String, replyTo: ActorRef[LockingResponse])   extends SupervisorLockMessage
  case class Unlock(prefix: String, token: String, replyTo: ActorRef[LockingResponse]) extends SupervisorLockMessage
}

sealed trait RunningMessage extends ComponentMessage with SupervisorRunningMessage
object RunningMessage {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage with ContainerExternalMessage
  trait DomainMessage                                        extends RunningMessage with SupervisorLockMessage
}

case object Shutdown extends SupervisorCommonMessage with ContainerCommonMessage
case object Restart  extends SupervisorCommonMessage with ContainerCommonMessage
////////////////////

sealed trait SupervisorMessage

sealed trait SupervisorExternalMessage extends SupervisorMessage with TMTSerializable
sealed trait SupervisorRunningMessage  extends SupervisorExternalMessage
sealed trait SupervisorRestartMessage  extends SupervisorMessage
object SupervisorRestartMessage {
  case object UnRegistrationComplete                    extends SupervisorRestartMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRestartMessage
}

sealed trait SupervisorCommonMessage extends SupervisorExternalMessage
object SupervisorCommonMessage {
  case class LifecycleStateSubscription(subscriberMessage: SubscriberMessage[LifecycleStateChanged])
      extends SupervisorCommonMessage
  case class ComponentStateSubscription(subscriberMessage: SubscriberMessage[CurrentState]) extends SupervisorCommonMessage
  case class GetSupervisorLifecycleState(replyTo: ActorRef[SupervisorLifecycleState])       extends SupervisorCommonMessage
}

sealed trait SupervisorIdleMessage extends SupervisorMessage
object SupervisorIdleMessage {
  case class RegistrationSuccess(componentRef: ActorRef[RunningMessage])     extends SupervisorIdleMessage
  case class RegistrationNotRequired(componentRef: ActorRef[RunningMessage]) extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable)                        extends SupervisorIdleMessage
  case object InitializeTimeout                                              extends SupervisorIdleMessage
}

sealed trait FromComponentLifecycleMessage extends SupervisorIdleMessage with SupervisorRunningMessage
object FromComponentLifecycleMessage {
  case class Running(componentRef: ActorRef[RunningMessage]) extends FromComponentLifecycleMessage
}

///////////////////
sealed trait ContainerMessage

sealed trait ContainerExternalMessage extends ContainerMessage with TMTSerializable

sealed trait ContainerCommonMessage extends ContainerExternalMessage
object ContainerCommonMessage {
  case class GetComponents(replyTo: ActorRef[Components])                           extends ContainerCommonMessage
  case class GetContainerLifecycleState(replyTo: ActorRef[ContainerLifecycleState]) extends ContainerCommonMessage
}

sealed trait ContainerIdleMessage extends ContainerMessage
object ContainerIdleMessage {
  case class SupervisorsCreated(supervisors: Set[SupervisorInfo]) extends ContainerIdleMessage
}

sealed trait FromSupervisorMessage extends ContainerIdleMessage
object FromSupervisorMessage {
  case class SupervisorLifecycleStateChanged(supervisor: ActorRef[SupervisorExternalMessage],
                                             supervisorLifecycleState: SupervisorLifecycleState)
      extends FromSupervisorMessage
}

////////////////

sealed trait CommandResponseManagerMessage
object CommandResponseManagerMessage {
  case class AddOrUpdateCommand(commandId: RunId, commandResponse: CommandResponse)  extends CommandResponseManagerMessage
  case class AddSubCommand(commandId: RunId, subCommandId: RunId)                    extends CommandResponseManagerMessage
  case class UpdateSubCommand(subCommandId: RunId, commandResponse: CommandResponse) extends CommandResponseManagerMessage
  case class Query(commandId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorExternalMessage
  case class Subscribe(commandId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorExternalMessage
  case class UnSubscribe(commandId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorExternalMessage

}
