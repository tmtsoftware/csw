package csw.messages

import akka.typed.ActorRef
import csw.messages.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.PubSub.SubscriberMessage
import csw.messages.framework._
import csw.messages.location.TrackingEvent
import csw.messages.params.models.{Id, Prefix}
import csw.messages.params.states.CurrentState

import scala.concurrent.duration.FiniteDuration

//TODO: explain better significance for each message hierarchy
sealed trait TopLevelActorMessage

sealed trait TopLevelActorCommonMessage extends TopLevelActorMessage
object TopLevelActorCommonMessage {
  case class UnderlyingHookFailed(throwable: Throwable)          extends TopLevelActorCommonMessage
  case class TrackingEventReceived(trackingEvent: TrackingEvent) extends TopLevelActorCommonMessage
}

sealed trait TopLevelActorIdleMessage extends TopLevelActorMessage
object TopLevelActorIdleMessage {
  case object Initialize extends TopLevelActorIdleMessage
}

sealed trait CommandMessage extends RunningMessage with SupervisorLockMessage {
  def replyTo: ActorRef[CommandResponse]
  def command: ControlCommand
}

object CommandMessage {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
}

case class LockTimedout(replyTo: ActorRef[LockingResponse])       extends SupervisorMessage
case class LockAboutToTimeout(replyTo: ActorRef[LockingResponse]) extends SupervisorMessage

sealed trait SupervisorLockMessage extends SupervisorRunningMessage
object SupervisorLockMessage {
  case class Lock(source: Prefix, replyTo: ActorRef[LockingResponse], leaseDuration: FiniteDuration) extends SupervisorLockMessage
  case class Unlock(source: Prefix, replyTo: ActorRef[LockingResponse])                              extends SupervisorLockMessage
}

sealed trait RunningMessage extends TopLevelActorMessage with SupervisorRunningMessage
object RunningMessage {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage with ContainerExternalMessage
}

sealed trait CommonMessage extends ComponentCommonMessage with ContainerCommonMessage
object SupervisorContainerCommonMessages {
  case object Shutdown extends CommonMessage
  case object Restart  extends CommonMessage

  def jShutdown(): CommonMessage = Shutdown
  def jRestart(): CommonMessage  = Restart
}
////////////////////

sealed trait SupervisorMessage

sealed trait ComponentMessage         extends SupervisorMessage with TMTSerializable
sealed trait SupervisorRunningMessage extends ComponentMessage

sealed trait SupervisorInternalRunningMessage extends SupervisorMessage
object SupervisorInternalRunningMessage {
  case class RegistrationSuccess(componentRef: ActorRef[RunningMessage])     extends SupervisorInternalRunningMessage
  case class RegistrationNotRequired(componentRef: ActorRef[RunningMessage]) extends SupervisorInternalRunningMessage
  case class RegistrationFailed(throwable: Throwable)                        extends SupervisorInternalRunningMessage
}

sealed trait SupervisorRestartMessage extends SupervisorMessage
object SupervisorRestartMessage {
  case object UnRegistrationComplete                    extends SupervisorRestartMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRestartMessage
}

sealed trait ComponentCommonMessage extends ComponentMessage
object ComponentCommonMessage {
  case class LifecycleStateSubscription(subscriberMessage: SubscriberMessage[LifecycleStateChanged])
      extends ComponentCommonMessage
  case class ComponentStateSubscription(subscriberMessage: SubscriberMessage[CurrentState]) extends ComponentCommonMessage
  case class GetSupervisorLifecycleState(replyTo: ActorRef[SupervisorLifecycleState])       extends ComponentCommonMessage
}

sealed trait SupervisorIdleMessage extends SupervisorMessage
object SupervisorIdleMessage {
  case object InitializeTimeout extends SupervisorIdleMessage
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
  case class SupervisorLifecycleStateChanged(supervisor: ActorRef[ComponentMessage],
                                             supervisorLifecycleState: SupervisorLifecycleState)
      extends FromSupervisorMessage
}

////////////////

sealed trait CommandResponseManagerMessage
object CommandResponseManagerMessage {
  case class AddOrUpdateCommand(commandId: Id, commandResponse: CommandResponse)  extends CommandResponseManagerMessage
  case class AddSubCommand(commandId: Id, subCommandId: Id)                       extends CommandResponseManagerMessage
  case class UpdateSubCommand(subCommandId: Id, commandResponse: CommandResponse) extends CommandResponseManagerMessage
  case class Query(commandId: Id, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorLockMessage
  case class Subscribe(commandId: Id, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorLockMessage
  case class Unsubscribe(commandId: Id, replyTo: ActorRef[CommandResponse])
      extends CommandResponseManagerMessage
      with SupervisorLockMessage
}
