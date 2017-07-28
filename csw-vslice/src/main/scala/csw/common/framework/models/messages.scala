package csw.common.framework.models

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.param.Parameters.{ControlCommand, Setup}
import csw.param.StateVariable.CurrentState
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

sealed trait HcdResponseMode

object HcdResponseMode {
  case object Idle extends HcdResponseMode
  case class Initialized(hcdRef: ActorRef[InitialHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]])
      extends HcdResponseMode
  case class Running(hcdRef: ActorRef[RunningHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]]) extends HcdResponseMode
}

///////////////

sealed trait FromComponentLifecycleMessage extends HcdResponseMode with AssemblyResponseMode

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

trait DomainMsg

///////////////

sealed trait HcdMsg

sealed trait IdleHcdMsg extends HcdMsg
object IdleHcdMsg {
  case object Initialize extends IdleHcdMsg
  case object Start      extends IdleHcdMsg
}

sealed trait InitialHcdMsg extends HcdMsg
object InitialHcdMsg {
  case object Run extends InitialHcdMsg
}

sealed trait RunningHcdMsg extends HcdMsg
object RunningHcdMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningHcdMsg
  case class Submit(command: Setup)                          extends RunningHcdMsg
  case class DomainHcdMsg[T <: DomainMsg](msg: T)            extends RunningHcdMsg
}

//////////////////////////

sealed trait AssemblyResponseMode

object AssemblyResponseMode {
  case object Idle                                                  extends AssemblyResponseMode
  case class Initialized(assemblyRef: ActorRef[InitialAssemblyMsg]) extends AssemblyResponseMode
  case class Running(assemblyRef: ActorRef[RunningAssemblyMsg])     extends AssemblyResponseMode
}

//////////////////////////
sealed trait AssemblyMsg

sealed trait IdleAssemblyMsg extends AssemblyMsg
object IdleAssemblyMsg {
  case object Initialize extends IdleAssemblyMsg
  case object Start      extends IdleAssemblyMsg
}

sealed trait InitialAssemblyMsg extends AssemblyMsg
object InitialAssemblyMsg {
  case object Run extends InitialAssemblyMsg
}

sealed trait RunningAssemblyMsg extends AssemblyMsg
object RunningAssemblyMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage)                     extends RunningAssemblyMsg
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsg
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsg
  case class DomainAssemblyMsg[T <: DomainMsg](msg: T)                           extends RunningAssemblyMsg
}

///////////////////////
sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
  case object StopCurrentCommand                              extends CommandMsgs
  case class SetStateResponseE(response: StateWasSet)         extends CommandMsgs
}
