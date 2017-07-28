package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.IdleHcdMsg.{Initialize, Start}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class HcdBehavior[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg],
                                              supervisor: ActorRef[HcdResponseMode],
                                              hcdHandlers: HcdHandlers[Msg])
    extends Actor.MutableBehavior[HcdMsg] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: HcdResponseMode = Idle
  ctx.self ! Initialize

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (mode, msg) match {
      case (Idle, x: IdleHcdMsg)              ⇒ onIdle(x)
      case (_: Initialized, x: InitialHcdMsg) ⇒ onInitial(x)
      case (_: Running, x: RunningHcdMsg)     ⇒ onRunning(x)
      case _                                  ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def initialization(): Future[Unit] = async {
    await(hcdHandlers.initialize())
    mode = Initialized(ctx.self, hcdHandlers.pubSubRef)
  }

  private def onIdle(x: IdleHcdMsg): Unit = x match {
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

  private def onInitial(x: InitialHcdMsg): Unit = x match {
    case Run =>
      hcdHandlers.onRun()
      val running = Running(ctx.self, hcdHandlers.pubSubRef)
      mode = running
      hcdHandlers.isOnline = true
      supervisor ! running
  }

  private def onRunning(x: RunningHcdMsg): Unit = x match {
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => hcdHandlers.onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ hcdHandlers.onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      hcdHandlers.onShutdown()
      hcdHandlers.stopChildren()
      supervisor ! ShutdownComplete
    case Restart =>
      hcdHandlers.onRestart()
      mode = Idle
      ctx.self ! Start
    case GoOnline =>
      if (!hcdHandlers.isOnline) {
        hcdHandlers.onGoOnline()
        hcdHandlers.isOnline = true
      }
    case GoOffline =>
      if (hcdHandlers.isOnline) {
        hcdHandlers.onGoOffline()
        hcdHandlers.isOnline = false
      }
  }
}
