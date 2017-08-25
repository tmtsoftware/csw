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
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMsg with RunningContainerMsg
  trait DomainMsg                                            extends RunningMsg
}

///////////////

sealed trait CommonSupervisorMsg extends SupervisorExternalMessage
object CommonSupervisorMsg {
  case class LifecycleStateSubscription(subscriberMsg: SubscriberMsg[LifecycleStateChanged]) extends CommonSupervisorMsg
  case class ComponentStateSubscription(subscriberMsg: SubscriberMsg[CurrentState])          extends CommonSupervisorMsg
  case object HaltComponent                                                                  extends CommonSupervisorMsg
}

sealed trait SupervisorIdleComponentMsg extends FromComponentLifecycleMessage with SupervisorIdleMessage
object SupervisorIdleComponentMsg {
  case class Initialized(componentRef: ActorRef[InitialMsg]) extends SupervisorIdleComponentMsg
  case class InitializeFailure(reason: String)               extends SupervisorIdleComponentMsg
  case class Running(componentRef: ActorRef[RunningMsg])     extends SupervisorIdleComponentMsg
}

sealed trait PreparingToShutdownMsg extends SupervisorMsg
object PreparingToShutdownMsg {
  case object ShutdownTimeout                extends PreparingToShutdownMsg
  case class ShutdownFailure(reason: String) extends PreparingToShutdownMsg with FromComponentLifecycleMessage
  case object ShutdownComplete               extends PreparingToShutdownMsg with FromComponentLifecycleMessage
}

object SupervisorIdleMessage {
  case class RegistrationComplete(registrationResult: RegistrationResult, componentRef: ActorRef[InitialMsg])
      extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable) extends SupervisorIdleMessage
}

object SupervisorRunningMessage {
  case object UnRegistrationComplete                    extends SupervisorRunningMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRunningMessage
}
sealed trait FromComponentLifecycleMessage extends SupervisorMsg
sealed trait SupervisorExternalMessage     extends SupervisorMsg
sealed trait SupervisorIdleMessage         extends SupervisorMsg
sealed trait SupervisorRunningMessage      extends SupervisorMsg
sealed trait SupervisorMsg

///////////////

sealed trait ContainerMsg
sealed trait CommonContainerMsg extends ContainerMsg
object CommonContainerMsg {
  case class GetComponents(replyTo: ActorRef[Components])       extends CommonContainerMsg
  case class GetContainerMode(replyTo: ActorRef[ContainerMode]) extends CommonContainerMsg
}

sealed trait IdleContainerMsg extends ContainerMsg
object IdleContainerMsg {
  case class SupervisorModeChanged(lifecycleStateChanged: LifecycleStateChanged) extends IdleContainerMsg
  case class RegistrationComplete(registrationResult: RegistrationResult)        extends IdleContainerMsg
  case class RegistrationFailed(throwable: Throwable)                            extends IdleContainerMsg
}

sealed trait RunningContainerMsg extends ContainerMsg
object RunningContainerMsg {
  case object UnRegistrationComplete                    extends RunningContainerMsg
  case class UnRegistrationFailed(throwable: Throwable) extends RunningContainerMsg
}

case class LifecycleStateChanged(state: SupervisorMode, publisher: ActorRef[SupervisorExternalMessage])

case class Components(components: List[SupervisorInfo])

case class SupervisorInfo(system: ActorSystem,
                          supervisor: ActorRef[SupervisorExternalMessage],
                          componentInfo: ComponentInfo)
