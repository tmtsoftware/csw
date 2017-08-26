package csw.common.framework.internal

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus
import csw.common.framework.models.CommandMessage.{Oneway, Submit}
import csw.common.framework.models.IdleMessage.{Initialize, Start}
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.PreparingToShutdownMessage.{ShutdownComplete, ShutdownFailure}
import csw.common.framework.models.RunningMessage.{DomainMessage, Lifecycle}
import csw.common.framework.models.SupervisorIdleComponentMessage.InitializeFailure
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models.{RunningMessage, _}
import csw.common.framework.scaladsl.ComponentHandlers

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ComponentBehavior[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers[Msg]
) extends Actor.MutableBehavior[ComponentMessage] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentMode = ComponentMode.Idle

  ctx.self ! Initialize

  def onMessage(msg: ComponentMessage): Behavior[ComponentMessage] = {
    (mode, msg) match {
      case (ComponentMode.Idle, x: IdleMessage)           ⇒ onIdle(x)
      case (ComponentMode.Initialized, x: InitialMessage) ⇒ onInitial(x)
      case (ComponentMode.Running, x: RunningMessage)     ⇒ onRun(x)
      case _                                              ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  private def onIdle(x: IdleMessage): Unit = x match {
    case Initialize =>
      async {
        await(initialization())
        supervisor ! SupervisorIdleComponentMessage.Initialized(ctx.self)
      } recover {
        case NonFatal(ex) ⇒ supervisor ! InitializeFailure(ex.getMessage)
      }
    case Start ⇒
      async {
        await(initialization())
        ctx.self ! Run
      } recover {
        case NonFatal(ex) ⇒ supervisor ! InitializeFailure(ex.getMessage)
      }
  }

  private def initialization(): Future[Unit] =
    async {
      mode = ComponentMode.Initialized
      await(lifecycleHandlers.initialize())
    }

  private def onInitial(x: InitialMessage): Unit = x match {
    case Run =>
      mode = ComponentMode.Running
      lifecycleHandlers.onRun()
      lifecycleHandlers.isOnline = true
      supervisor ! SupervisorIdleComponentMessage.Running(ctx.self)
  }

  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) => onLifecycle(message)
    case x: Msg             => lifecycleHandlers.onDomainMsg(x)
    case x: CommandMessage  => onRunningCompCommandMessage(x)
    case _                  ⇒ println("wrong msg")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      try {
        lifecycleHandlers.onShutdown()
        supervisor ! ShutdownComplete
      } catch {
        case ex: Exception ⇒ supervisor ! ShutdownFailure(ex.getMessage)
      }
    case Restart =>
      mode = ComponentMode.Idle
      lifecycleHandlers.onRestart()
      ctx.self ! Start
    case GoOnline =>
      if (!lifecycleHandlers.isOnline) {
        lifecycleHandlers.onGoOnline()
        lifecycleHandlers.isOnline = true
      }
    case GoOffline =>
      if (lifecycleHandlers.isOnline) {
        lifecycleHandlers.onGoOffline()
        lifecycleHandlers.isOnline = false
      }
  }

  def onRunningCompCommandMessage(message: CommandMessage): Unit = {
    val newMessage: CommandMessage = message match {
      case x: Oneway ⇒ x.copy(replyTo = ctx.spawnAnonymous(Actor.ignore))
      case x: Submit ⇒ x
    }
    val validation              = lifecycleHandlers.onControlCommand(newMessage)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    message.replyTo ! validationCommandResult
  }

}
