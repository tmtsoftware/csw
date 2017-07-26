package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.IdleHcdMsg.{Initialize, Start}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models.ToComponentLifecycleMessage.{
  GoOffline,
  GoOnline,
  LifecycleFailureInfo,
  Restart,
  Shutdown
}
import csw.common.framework.models.{ToComponentLifecycleMessage, _}
import csw.common.framework.scaladsl.PubSubActor
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class HcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode])
    extends Actor.MutableBehavior[HcdMsg] {

  val domainAdapter: ActorRef[Msg] = ctx.spawnAdapter(DomainHcdMsg.apply)

  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: HcdResponseMode = Idle
  ctx.self ! Initialize

  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onSetup(sc: Setup): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit

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
    await(initialize())
    mode = Initialized(ctx.self, pubSubRef)
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
      onRun()
      val running = Running(ctx.self, pubSubRef)
      mode = running
      isOnline = true
      supervisor ! running
  }

  private def onRunning(x: RunningHcdMsg): Unit = x match {
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      onShutdown()
      ctx.stop(domainAdapter)
      ctx.stop(pubSubRef)
      supervisor ! ShutdownComplete
    case Restart =>
      onRestart()
      mode = Idle
      ctx.self ! Start
    case GoOnline =>
      if (!isOnline) {
        onGoOnline()
        isOnline = true
      }
    case GoOffline =>
      if (isOnline) {
        onGoOffline()
        isOnline = false
      }
    case LifecycleFailureInfo(state, reason) => onLifecycleFailureInfo(state, reason)
  }
}
