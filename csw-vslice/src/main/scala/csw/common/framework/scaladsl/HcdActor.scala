package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.IdleHcdMsg.Initialize
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models.ToComponentLifecycleMessage.{
  LifecycleFailureInfo,
  Restart,
  RunOffline,
  RunOnline,
  Shutdown
}
import csw.common.framework.models.{ToComponentLifecycleMessage, _}
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] {

  protected def make(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]): HcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx, supervisor)).narrow
}

abstract class HcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode])
    extends Actor.MutableBehavior[HcdMsg] {

  val domainAdapter: ActorRef[Msg] = ctx.spawnAdapter(DomainHcdMsg.apply)

  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: HcdResponseMode = Idle

  ctx.self ! Initialize

  def initialize(): Future[Unit]
  def onInitialRun(): Unit
  def onInitialHcdShutdownComplete(): Unit
  def onRunningHcdShutdownComplete(): Unit
  def onSetup(sc: Setup): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onRunOnline(): Unit
  def onRunOffline(): Unit
  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit
  def onShutdownComplete(): Unit

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (mode, msg) match {
      case (Idle, x: IdleHcdMsg)              ⇒ onIdle(x)
      case (_: Initialized, x: InitialHcdMsg) ⇒ onInitial(x)
      case (_: Running, x: RunningHcdMsg)     ⇒ onRunning(x)
      case _                                  ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  private def onIdle(x: IdleHcdMsg): Unit = x match {
    case Initialize =>
      async {
        await(initialize())
        mode = Initialized(ctx.self, pubSubRef)
        supervisor ! mode
      }
  }

  private def onInitial(x: InitialHcdMsg): Unit = x match {
    case Run(replyTo) =>
      onInitialRun()
      val running = Running(ctx.self, pubSubRef)
      mode = running
      replyTo ! running
    case HcdShutdownComplete =>
      onInitialHcdShutdownComplete()
  }

  private def onRunning(x: RunningHcdMsg): Unit = x match {
    case HcdShutdownComplete  => onRunningHcdShutdownComplete()
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      onShutdown()
      supervisor ! ShutdownComplete
      val b1 = ctx.stop(domainAdapter)
      val b2 = ctx.stop(pubSubRef)
      val b3 = ctx.stop(supervisor)
      println((b1, b2, b3))
    case Restart =>
      onRestart()
      mode = Idle
      ctx.self ! Initialize
    case RunOnline =>
      onRunOnline()
      mode = Running(ctx.self, pubSubRef)
    case RunOffline =>
      onRunOffline()
      mode = Idle
    case LifecycleFailureInfo(state, reason) => onLifecycleFailureInfo(state, reason)
  }
}
