package csw.common.framework

import akka.typed.ActorRef
import csw.param.Parameters.{ControlCommand, Setup}
import csw.param.StateVariable.CurrentState
import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.HcdComponentLifecycleMessage.Running

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
  case object DoShutdown                                                 extends ToComponentLifecycleMessage
  case object DoRestart                                                  extends ToComponentLifecycleMessage
  case object Running                                                    extends ToComponentLifecycleMessage
  case object RunningOffline                                             extends ToComponentLifecycleMessage
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessage
}

///////////////

sealed trait HcdComponentLifecycleMessage

object HcdComponentLifecycleMessage {
  case class Initialized(hcdRef: ActorRef[InitialHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]])
      extends HcdComponentLifecycleMessage
  case class Running(hcdRef: ActorRef[RunningHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]])
      extends HcdComponentLifecycleMessage
}

sealed trait AssemblyComponentLifecycleMessage

object AssemblyComponentLifecycleMessage {
  case class Initialized(assemblyRef: ActorRef[InitialAssemblyMsg]) extends AssemblyComponentLifecycleMessage
  case class Running(assemblyRef: ActorRef[RunningAssemblyMsg])     extends AssemblyComponentLifecycleMessage
}

sealed trait FromComponentLifecycleMessage extends HcdComponentLifecycleMessage with AssemblyComponentLifecycleMessage

object FromComponentLifecycleMessage {
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessage
  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessage
  case object HaltComponent                    extends FromComponentLifecycleMessage
}

case object ShutdownComplete extends FromComponentLifecycleMessage with ToComponentLifecycleMessage

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

sealed trait InitialHcdMsg extends HcdMsg
object InitialHcdMsg {
  case class Run(replyTo: ActorRef[Running]) extends InitialHcdMsg
}

sealed trait RunningHcdMsg extends HcdMsg
object RunningHcdMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningHcdMsg
  case class Submit(command: Setup)                          extends RunningHcdMsg
  case class DomainHcdMsg[T <: DomainMsg](msg: T)            extends RunningHcdMsg
}

case object HcdShutdownComplete extends InitialHcdMsg with RunningHcdMsg

//////////////////////////
sealed trait AssemblyMsg
sealed trait InitialAssemblyMsg extends AssemblyMsg
object InitialAssemblyMsg {
  case class Run(replyTo: ActorRef[AssemblyComponentLifecycleMessage.Running]) extends InitialAssemblyMsg
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
