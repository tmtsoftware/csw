package csw.common.framework.models

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.internal.{ContainerMode, SupervisorMode}
import csw.common.framework.models.PubSub.SubscriberMsg
import csw.param.commands.ControlCommand
import csw.param.states.CurrentState
import csw.services.location.models.RegistrationResult

/////////////

sealed trait PubSub[T]
object PubSub {
  sealed trait SubscriberMsg[T]               extends PubSub[T]
  case class Subscribe[T](ref: ActorRef[T])   extends SubscriberMsg[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends SubscriberMsg[T]

  sealed trait PublisherMsg[T]   extends PubSub[T]
  case class Publish[T](data: T) extends PublisherMsg[T]
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

sealed trait ComponentMsg

sealed trait IdleMsg extends ComponentMsg
object IdleMsg {
  case object Initialize extends IdleMsg
  case object Start      extends IdleMsg
}

sealed trait InitialMsg extends ComponentMsg
object InitialMsg {
  case object Run extends InitialMsg
}

sealed trait CommandMsg extends RunningMsg {
  def command: ControlCommand
  def replyTo: ActorRef[CommandResponse]
}
object CommandMsg {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMsg
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMsg
}

sealed trait RunningMsg extends ComponentMsg with SupervisorExternalMessage with SupervisorRunningMessage
object RunningMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage)
      extends RunningMsg
      with ContainerExternalMessage
      with ContainerRunningMsg
  trait DomainMsg extends RunningMsg
}

///////////////

sealed trait SupervisorMsg
sealed trait SupervisorExternalMessage     extends SupervisorMsg
sealed trait FromComponentLifecycleMessage extends SupervisorMsg

sealed trait SupervisorCommonMsg extends SupervisorExternalMessage
object SupervisorCommonMsg {
  case class LifecycleStateSubscription(subscriberMsg: SubscriberMsg[LifecycleStateChanged]) extends SupervisorCommonMsg
  case class ComponentStateSubscription(subscriberMsg: SubscriberMsg[CurrentState])          extends SupervisorCommonMsg
  case object HaltComponent                                                                  extends SupervisorCommonMsg
}

sealed trait SupervisorIdleMessage extends SupervisorMsg
object SupervisorIdleMessage {
  case class RegistrationComplete(registrationResult: RegistrationResult, componentRef: ActorRef[InitialMsg])
      extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable) extends SupervisorIdleMessage
}

sealed trait SupervisorIdleComponentMsg extends FromComponentLifecycleMessage with SupervisorIdleMessage
object SupervisorIdleComponentMsg {
  case class Initialized(componentRef: ActorRef[InitialMsg]) extends SupervisorIdleComponentMsg
  case class InitializeFailure(reason: String)               extends SupervisorIdleComponentMsg
  case class Running(componentRef: ActorRef[RunningMsg])     extends SupervisorIdleComponentMsg
}

sealed trait SupervisorRunningMessage extends SupervisorMsg
object SupervisorRunningMessage {
  case object UnRegistrationComplete                    extends SupervisorRunningMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRunningMessage
}

sealed trait PreparingToShutdownMsg extends SupervisorMsg
object PreparingToShutdownMsg {
  case object ShutdownTimeout                extends PreparingToShutdownMsg
  case class ShutdownFailure(reason: String) extends PreparingToShutdownMsg with FromComponentLifecycleMessage
  case object ShutdownComplete               extends PreparingToShutdownMsg with FromComponentLifecycleMessage
}

///////////////

sealed trait ContainerMsg
sealed trait ContainerExternalMessage extends ContainerMsg

sealed trait ContainerCommonMsg extends ContainerExternalMessage
object ContainerCommonMsg {
  case class GetComponents(replyTo: ActorRef[Components])       extends ContainerCommonMsg
  case class GetContainerMode(replyTo: ActorRef[ContainerMode]) extends ContainerCommonMsg
}

sealed trait ContainerIdleMsg extends ContainerMsg
object ContainerIdleMsg {
  case class SupervisorModeChanged(lifecycleStateChanged: LifecycleStateChanged) extends ContainerIdleMsg
  case class RegistrationComplete(registrationResult: RegistrationResult)        extends ContainerIdleMsg
  case class RegistrationFailed(throwable: Throwable)                            extends ContainerIdleMsg
}

sealed trait ContainerRunningMsg extends ContainerMsg
object ContainerRunningMsg {
  case object UnRegistrationComplete                    extends ContainerRunningMsg
  case class UnRegistrationFailed(throwable: Throwable) extends ContainerRunningMsg
}

case class LifecycleStateChanged(state: SupervisorMode, publisher: ActorRef[SupervisorExternalMessage])

case class Components(components: List[SupervisorInfo])

case class SupervisorInfo(system: ActorSystem,
                          supervisor: ActorRef[SupervisorExternalMessage],
                          componentInfo: ComponentInfo)
