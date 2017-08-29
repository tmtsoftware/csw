package csw.common.framework.models

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.models.PubSub.SubscriberMessage
import csw.param.commands.ControlCommand
import csw.param.states.CurrentState
import csw.services.location.models.{RegistrationResult, TmtSerializable}

/////////////

sealed trait PubSub[T]
object PubSub {
  sealed trait SubscriberMessage[T]           extends PubSub[T]
  case class Subscribe[T](ref: ActorRef[T])   extends SubscriberMessage[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends SubscriberMessage[T]

  sealed trait PublisherMessage[T] extends PubSub[T]
  case class Publish[T](data: T)   extends PublisherMessage[T]
}

///////////////

sealed trait ToComponentLifecycleMessage
object ToComponentLifecycleMessage {
  case object Shutdown  extends ToComponentLifecycleMessage
  case object Restart   extends ToComponentLifecycleMessage
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage
}

///////////////

sealed trait ComponentMessage

sealed trait IdleMessage extends ComponentMessage
object IdleMessage {
  case object Initialize extends IdleMessage
  case object Start      extends IdleMessage
}

sealed trait InitialMessage extends ComponentMessage
object InitialMessage {
  case object Run extends InitialMessage
}

sealed trait CommandMessage extends RunningMessage {
  def command: ControlCommand
  def replyTo: ActorRef[CommandResponse]
}
object CommandMessage {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
}

sealed trait RunningMessage extends ComponentMessage with SupervisorExternalMessage with SupervisorRunningMessage
object RunningMessage {
  case class Lifecycle(message: ToComponentLifecycleMessage)
      extends RunningMessage
      with ContainerExternalMessage
      with ContainerRunningMessage
  trait DomainMessage extends RunningMessage
}

///////////////

sealed trait SupervisorMessage
sealed trait SupervisorExternalMessage     extends SupervisorMessage with TmtSerializable
sealed trait FromComponentLifecycleMessage extends SupervisorMessage

sealed trait SupervisorCommonMessage extends SupervisorExternalMessage
object SupervisorCommonMessage {
  case class LifecycleStateSubscription(subscriberMessage: SubscriberMessage[LifecycleStateChanged])
      extends SupervisorCommonMessage
  case class ComponentStateSubscription(subscriberMessage: SubscriberMessage[CurrentState])
      extends SupervisorCommonMessage
  case object HaltComponent                                       extends SupervisorCommonMessage
  case class GetSupervisorMode(replyTo: ActorRef[SupervisorMode]) extends SupervisorCommonMessage
}

sealed trait SupervisorIdleMessage extends SupervisorMessage
object SupervisorIdleMessage {
  case class RegistrationComplete(registrationResult: RegistrationResult, componentRef: ActorRef[InitialMessage])
      extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable) extends SupervisorIdleMessage
}

sealed trait SupervisorIdleComponentMessage extends FromComponentLifecycleMessage with SupervisorIdleMessage
object SupervisorIdleComponentMessage {
  case class Initialized(componentRef: ActorRef[InitialMessage]) extends SupervisorIdleComponentMessage
  case class InitializeFailure(reason: String)                   extends SupervisorIdleComponentMessage
  case class Running(componentRef: ActorRef[RunningMessage])     extends SupervisorIdleComponentMessage
}

sealed trait SupervisorRunningMessage extends SupervisorMessage
object SupervisorRunningMessage {
  case object UnRegistrationComplete                    extends SupervisorRunningMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRunningMessage
}

sealed trait PreparingToShutdownMessage extends SupervisorMessage
object PreparingToShutdownMessage {
  case object ShutdownTimeout                extends PreparingToShutdownMessage
  case class ShutdownFailure(reason: String) extends PreparingToShutdownMessage with FromComponentLifecycleMessage
  case object ShutdownComplete               extends PreparingToShutdownMessage with FromComponentLifecycleMessage
}

///////////////

sealed trait ContainerMessage extends TmtSerializable

sealed trait ContainerCommonMessage extends ContainerMessage
object ContainerCommonMessage {
  case class GetContainerMode(replyTo: ActorRef[ContainerMode]) extends ContainerCommonMessage
}

sealed trait ContainerExternalMessage extends ContainerMessage
object ContainerExternalMessage {
  case class GetComponents(replyTo: ActorRef[Components]) extends ContainerExternalMessage with ContainerCommonMessage
}

sealed trait ContainerIdleMessage extends ContainerMessage
object ContainerIdleMessage {
  case class RegistrationComplete(registrationResult: RegistrationResult) extends ContainerIdleMessage
  case class RegistrationFailed(throwable: Throwable)                     extends ContainerIdleMessage
  case class SupervisorModeChanged(supervisor: ActorRef[SupervisorExternalMessage], supervisorMode: SupervisorMode)
      extends ContainerIdleMessage
}

sealed trait ContainerRunningMessage extends ContainerMessage
object ContainerRunningMessage {
  case object UnRegistrationComplete                    extends ContainerRunningMessage
  case class UnRegistrationFailed(throwable: Throwable) extends ContainerRunningMessage
}

case class LifecycleStateChanged(publisher: ActorRef[SupervisorExternalMessage], state: SupervisorMode)

case class Components(components: List[Component]) extends TmtSerializable

case class Component(supervisor: ActorRef[SupervisorExternalMessage], info: ComponentInfo)

case class SupervisorInfo(system: ActorSystem, component: Component)
