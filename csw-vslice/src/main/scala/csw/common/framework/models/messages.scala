package csw.common.framework.models

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.param.Parameters.{ControlCommand, Setup}
import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet

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

sealed trait FromComponentLifecycleMessage extends ComponentResponseMode
object FromComponentLifecycleMessage {
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessage
  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessage
  case object HaltComponent                    extends FromComponentLifecycleMessage
  case object ShutdownComplete                 extends FromComponentLifecycleMessage
}

/////////////

sealed trait PubSub[T]

object PubSub {
  case class Subscribe[T](ref: ActorRef[T])   extends PubSub[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends PubSub[T]
  case class Publish[T](data: T)              extends PubSub[T]
}

///////////////

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
  case object StopCurrentCommand                              extends CommandMsgs
  case class SetStateResponseE(response: StateWasSet)         extends CommandMsgs
}

///////////////

sealed trait ComponentResponseMode
object ComponentResponseMode {
  case object Idle                                           extends ComponentResponseMode
  case class Initialized(componentRef: ActorRef[InitialMsg]) extends ComponentResponseMode
  case class Running(componentRef: ActorRef[RunningMsg])     extends ComponentResponseMode
}

///////////////

sealed trait ComponentMsg

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
