package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.ComponentResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.FromComponentLifecycleMessage.{InitializeFailure, ShutdownComplete}
import csw.common.framework.models.IdleMsg.{Initialize, Start}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.RunningMsg.{DomainMsg, Lifecycle}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models.{RunningMsg, _}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

abstract class ComponentBehavior[Msg <: DomainMsg: ClassTag, RunCompMsg <: CompSpecificMsg: ClassTag](
    ctx: ActorContext[ComponentMsg],
    supervisor: ActorRef[ComponentResponseMode],
    lifecycleHandlers: LifecycleHandlers[Msg]
) extends Actor.MutableBehavior[ComponentMsg] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentResponseMode = Idle
  ctx.self ! Initialize

  def onRunningCompCommandMsg(x: RunCompMsg): Unit

  def onMessage(msg: ComponentMsg): Behavior[ComponentMsg] = {
    (mode, msg) match {
      case (Idle, x: IdleMsg)              ⇒ onIdle(x)
      case (_: Initialized, x: InitialMsg) ⇒ onInitial(x)
      case (_: Running, x: RunningMsg)     ⇒ onRun(x)
      case _                               ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  private def onIdle(x: IdleMsg): Unit = x match {
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

  private def initialization(): Future[Unit] =
    async {
      await(lifecycleHandlers.initialize())
      mode = Initialized(ctx.self)
    } recover {
      case NonFatal(ex) ⇒ supervisor ! InitializeFailure(ex.getMessage)
    }

  private def onInitial(x: InitialMsg): Unit = x match {
    case Run =>
      lifecycleHandlers.onRun()
      val running = Running(ctx.self)
      mode = running
      lifecycleHandlers.isOnline = true
      supervisor ! running
  }

  private def onRun(runningMsg: RunningMsg): Unit = runningMsg match {
    case Lifecycle(message) => onLifecycle(message)
    case x: Msg             => lifecycleHandlers.onDomainMsg(x)
    case x: RunCompMsg      => onRunningCompCommandMsg(x)
    case _                  ⇒ println("wrong msg")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      lifecycleHandlers.onShutdown()
      supervisor ! ShutdownComplete
    case Restart =>
      lifecycleHandlers.onRestart()
      mode = Idle
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
}
