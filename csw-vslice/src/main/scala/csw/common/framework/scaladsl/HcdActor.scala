package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{javadsl, ActorRef, Behavior}
import csw.common.framework.scaladsl.HcdActor.Mode
import csw.common.framework.models.HcdComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models._
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] {

  def make(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage]): HcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx, supervisor)).narrow
}

object HcdActor {
  sealed trait Mode
  object Mode {
    case object Initial extends Mode
    case object Running extends Mode
  }
}

abstract class HcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg],
                                                    supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends Actor.MutableBehavior[HcdMsg] {

  def this(ctx: javadsl.ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage]) =
    this(ctx.asScala, supervisor)

  val domainAdapter: ActorRef[Msg] = ctx.spawnAdapter(DomainHcdMsg.apply)

  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  var context: Mode = _

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onShutdown(): Unit
  def onShutdownComplete(): Unit
  def onLifecycle(x: ToComponentLifecycleMessage): Unit
  def onSetup(sc: Setup): Unit
  def onDomainMsg(msg: Msg): Unit

  async {
    await(initialize())
    supervisor ! Initialized(ctx.self, pubSubRef)
    context = Mode.Initial
  }

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (context, msg) match {
      case (Mode.Initial, x: InitialHcdMsg) ⇒ handleInitial(x)
      case (Mode.Running, x: RunningHcdMsg) ⇒ handleRunning(x)
      case _                                ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  private def handleInitial(x: InitialHcdMsg): Unit = x match {
    case Run(replyTo) =>
      onRun()
      context = Mode.Running
      replyTo ! Running(ctx.self, pubSubRef)
    case HcdShutdownComplete =>
      onShutdown()
  }

  private def handleRunning(x: RunningHcdMsg): Unit = x match {
    case HcdShutdownComplete  => onShutdownComplete()
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }
}
