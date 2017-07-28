package csw.common.framework.scaladsl.assembly

import akka.actor.Scheduler
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.models.AssemblyResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.IdleAssemblyMsg.{Initialize, Start}
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.models.RunningAssemblyMsg._
import csw.common.framework.models.ToComponentLifecycleMessage.{
  GoOffline,
  GoOnline,
  LifecycleFailureInfo,
  Restart,
  Shutdown
}
import csw.common.framework.models._
import csw.param.Parameters
import csw.param.Parameters.{Observe, Setup}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

class AssemblyBehavior[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg],
                                                   supervisor: ActorRef[AssemblyResponseMode],
                                                   assemblyHandlers: AssemblyHandlers[Msg])
    extends MutableBehavior[AssemblyMsg] {

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  val runningHcd: Option[HcdResponseMode.Running] = None

  var mode: AssemblyResponseMode = Idle

  ctx.self ! Initialize

  override def onMessage(msg: AssemblyMsg): Behavior[AssemblyMsg] = {
    (mode, msg) match {
      case (Idle, x: IdleAssemblyMsg)              ⇒ onIdle(x)
      case (_: Initialized, x: InitialAssemblyMsg) ⇒ onInitial(x)
      case (_: Running, x: RunningAssemblyMsg)     ⇒ onRunning(x)
      case _                                       ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def initialization(): Future[Unit] = async {
    await(assemblyHandlers.initialize())
    mode = Initialized(ctx.self)
  }

  private def onIdle(x: IdleAssemblyMsg): Unit = x match {
    case Initialize =>
      async {
        await(initialization())
        supervisor ! mode
      }
    case Start ⇒
      async {
        await(initialization())
        ctx.self ! Run
      }
  }

  def onInitial(x: InitialAssemblyMsg): Unit = x match {
    case Run =>
      assemblyHandlers.onRun()
      val running = Running(ctx.self)
      mode = running
      assemblyHandlers.isOnline = true
      supervisor ! running

  }

  def onRunning(x: RunningAssemblyMsg): Unit = x match {
    case Lifecycle(message)               => onLifecycle(message)
    case Submit(command, replyTo)         => onSubmit(command, replyTo)
    case Oneway(command, replyTo)         ⇒ onOneWay(command, replyTo)
    case DomainAssemblyMsg(diagMode: Msg) ⇒ assemblyHandlers.onDomainMsg(diagMode)
    case DomainAssemblyMsg(y)             ⇒ println(s"unhandled domain msg: $y")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      assemblyHandlers.onShutdown()
      supervisor ! ShutdownComplete
    case Restart =>
      assemblyHandlers.onRestart()
      mode = Idle
      ctx.self ! Start
    case GoOnline =>
      if (!assemblyHandlers.isOnline) {
        assemblyHandlers.onGoOnline()
        assemblyHandlers.isOnline = true
      }
    case GoOffline =>
      if (assemblyHandlers.isOnline) {
        assemblyHandlers.onGoOffline()
        assemblyHandlers.isOnline = false
      }
    case LifecycleFailureInfo(state, reason) => assemblyHandlers.onLifecycleFailureInfo(state, reason)
  }

  def onSubmit(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case si: Setup   => setupSubmit(si, oneway = false, replyTo)
    case oi: Observe => observeSubmit(oi, oneway = false, replyTo)
  }

  def onOneWay(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case sca: Setup   => setupSubmit(sca, oneway = true, replyTo)
    case oca: Observe => observeSubmit(oca, oneway = true, replyTo)
  }

  private def setupSubmit(s: Setup, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo       = if (oneway) None else Some(replyTo)
    val validation              = assemblyHandlers.setup(s, completionReplyTo)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  private def observeSubmit(o: Observe, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo = if (oneway) None else Some(replyTo)
    val validation        = assemblyHandlers.observe(o, completionReplyTo)

    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }
}
