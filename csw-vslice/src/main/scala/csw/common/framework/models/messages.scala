package csw.common.framework.models

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.models.SupervisorExternalMsg.LifecycleStateChanged
import csw.param.Parameters.{ControlCommand, Setup}

/////////////

sealed trait PubSub[T]

object PubSub {
  case class Subscribe[T](ref: ActorRef[T])   extends PubSub[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends PubSub[T]
  case class Publish[T](data: T)              extends PubSub[T]
}

/////////////

sealed trait LifecycleState

object LifecycleState {
  case object LifecycleWaitingForInitialized extends LifecycleState
  case object LifecycleInitializeFailure     extends LifecycleState
  case object LifecycleRunning               extends LifecycleState
  case object LifecycleRunningOffline        extends LifecycleState
  case object LifecyclePreparingToShutdown   extends LifecycleState
  case object LifecycleShutdown              extends LifecycleState
  case object LifecycleShutdownFailure       extends LifecycleState
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

sealed trait FromComponentLifecycleMessage extends SupervisorMsg

///////////////

sealed trait ComponentMsg

///////////////

sealed trait SupervisorMsg

///////////////

sealed trait IdleMsg extends ComponentMsg
object IdleMsg {
  case object Initialize extends IdleMsg
  case object Start      extends IdleMsg
}

///////////////

sealed trait InitialMsg extends ComponentMsg
object InitialMsg {
  case object Run extends InitialMsg
}

///////////////

sealed trait RunningMsg extends ComponentMsg
object RunningMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMsg
  trait DomainMsg                                            extends RunningMsg
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

sealed trait CompSpecificMsg extends RunningMsg

sealed trait HcdMsg extends CompSpecificMsg
object HcdMsg {
  case class Submit(command: Setup) extends HcdMsg
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

sealed trait AssemblyMsg extends CompSpecificMsg

object AssemblyMsg {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends AssemblyMsg
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends AssemblyMsg
}

///////////////////////

sealed trait SupervisorExternalMsg extends SupervisorMsg
object SupervisorExternalMsg {
  case class LifecycleStateChanged(state: LifecycleState) extends SupervisorExternalMsg
  case object ExComponentRestart                          extends SupervisorExternalMsg
  case object ExComponentShutdown                         extends SupervisorExternalMsg
  case object ExComponentOnline                           extends SupervisorExternalMsg
  case object ExComponentOffline                          extends SupervisorExternalMsg
}

sealed trait CommonSupervisorMsg extends SupervisorMsg
object CommonSupervisorMsg {
  case class SubscribeLifecycleCallback(actorRef: ActorRef[LifecycleStateChanged])   extends CommonSupervisorMsg
  case class UnsubscribeLifecycleCallback(actorRef: ActorRef[LifecycleStateChanged]) extends CommonSupervisorMsg
  case object HaltComponent                                                          extends CommonSupervisorMsg

}

sealed trait PreparingToShutdownMsg extends SupervisorMsg
object PreparingToShutdownMsg {
  case object ShutdownTimeout                extends PreparingToShutdownMsg
  case class ShutdownFailure(reason: String) extends PreparingToShutdownMsg
  case object ShutdownComplete               extends PreparingToShutdownMsg with FromComponentLifecycleMessage
}

sealed trait SupervisorIdleMsg extends FromComponentLifecycleMessage with SupervisorMsg
object SupervisorIdleMsg {
  case class Initialized(componentRef: ActorRef[InitialMsg]) extends SupervisorIdleMsg
  case class InitializeFailure(reason: String)               extends SupervisorIdleMsg
  case class Running(componentRef: ActorRef[RunningMsg])     extends SupervisorIdleMsg
}
