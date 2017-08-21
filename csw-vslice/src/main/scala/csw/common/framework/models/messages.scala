package csw.common.framework.models

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.internal.SupervisorMode
import csw.common.framework.models.PubSub.SubscriberMsg
import csw.param.commands.ControlCommand
import csw.param.states.CurrentState

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

sealed trait RunningMsg extends ComponentMsg with SupervisorExternalMessage
object RunningMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMsg with ContainerMsg
  trait DomainMsg                                            extends RunningMsg
}

///////////////

sealed trait CommonSupervisorMsg extends SupervisorExternalMessage
object CommonSupervisorMsg {
  case class LifecycleStateSubscription(subscriberMsg: SubscriberMsg[LifecycleStateChanged]) extends CommonSupervisorMsg
  case class ComponentStateSubscription(subscriberMsg: SubscriberMsg[CurrentState])          extends CommonSupervisorMsg
  case object HaltComponent                                                                  extends CommonSupervisorMsg
}

sealed trait SupervisorIdleMsg extends FromComponentLifecycleMessage
object SupervisorIdleMsg {
  case class Initialized(componentRef: ActorRef[InitialMsg]) extends SupervisorIdleMsg
  case class InitializeFailure(reason: String)               extends SupervisorIdleMsg
  case class Running(componentRef: ActorRef[RunningMsg])     extends SupervisorIdleMsg
}

sealed trait PreparingToShutdownMsg extends SupervisorMsg
object PreparingToShutdownMsg {
  case object ShutdownTimeout                extends PreparingToShutdownMsg
  case class ShutdownFailure(reason: String) extends PreparingToShutdownMsg with FromComponentLifecycleMessage
  case object ShutdownComplete               extends PreparingToShutdownMsg with FromComponentLifecycleMessage
}

sealed trait SupervisorExternalMessage     extends SupervisorMsg
sealed trait FromComponentLifecycleMessage extends SupervisorMsg
sealed trait SupervisorMsg

///////////////

sealed trait ContainerMsg
object ContainerMsg {
  case class GetComponents(replyTo: ActorRef[Components])                        extends ContainerMsg
  case class SupervisorModeChanged(lifecycleStateChanged: LifecycleStateChanged) extends ContainerMsg
}

case class LifecycleStateChanged(state: SupervisorMode, publisher: ActorRef[SupervisorMsg])

case class Components(components: List[SupervisorInfo])

case class SupervisorInfo(supervisor: ActorRef[SupervisorMsg], componentInfo: ComponentInfo)
