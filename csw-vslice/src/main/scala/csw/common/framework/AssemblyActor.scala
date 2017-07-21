package csw.common.framework

import akka.actor.Scheduler
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation.Validation
import csw.common.framework.AssemblyActor.Mode
import csw.common.framework.AssemblyComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.Component.AssemblyInfo
import csw.common.framework.InitialAssemblyMsg.Run
import csw.common.framework.RunningAssemblyMsg._
import csw.param.Parameters
import csw.param.Parameters.{Observe, Setup}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

object AssemblyActor {
  sealed trait Mode
  object Mode {
    case object Initial extends Mode
    case object Running extends Mode
  }
}

abstract class AssemblyActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg],
                                                         info: AssemblyInfo,
                                                         supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends MutableBehavior[AssemblyMsg] {

  val runningHcd: Option[HcdComponentLifecycleMessage.Running] = ???

  var mode: Mode = _

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  def initialize(): Future[Unit]
  def onRun(): Unit
  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation
  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation
  def onDomainMsg(msg: Msg): Unit
  def onLifecycle(message: ToComponentLifecycleMessage): Unit

  async {
    await(initialize())
    supervisor ! Initialized(ctx.self)
    mode = Mode.Initial
  }

  override def onMessage(msg: AssemblyMsg): Behavior[AssemblyMsg] = {
    (mode, msg) match {
      case (Mode.Initial, x: InitialAssemblyMsg) ⇒ handleInitial(x)
      case (Mode.Running, x: RunningAssemblyMsg) ⇒ handleRunning(x)
      case _                                     ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: InitialAssemblyMsg): Unit = x match {
    case Run(replyTo) =>
      onRun()
      mode = Mode.Running
      replyTo ! Running(ctx.self)
  }

  def handleRunning(x: RunningAssemblyMsg): Unit = x match {
    case Lifecycle(message)               => onLifecycle(message)
    case Submit(command, replyTo)         => onSubmit(command, replyTo)
    case Oneway(command, replyTo)         ⇒ onOneWay(command, replyTo)
    case DomainAssemblyMsg(diagMode: Msg) ⇒ onDomainMsg(diagMode)
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
    val validation              = setup(s, completionReplyTo)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  private def observeSubmit(o: Observe, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo = if (oneway) None else Some(replyTo)
    val validation        = observe(o, completionReplyTo)

    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }
}
