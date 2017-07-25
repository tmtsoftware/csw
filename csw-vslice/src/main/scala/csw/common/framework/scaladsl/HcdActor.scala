package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{javadsl, ActorRef, Behavior}
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models._
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

  def this(ctx: javadsl.ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]) =
    this(ctx.asScala, supervisor)

  val domainAdapter: ActorRef[Msg] = ctx.spawnAdapter(DomainHcdMsg.apply)

  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: HcdResponseMode = _

  def initialize(): Future[Unit]
  def onInitialRun(): Unit
  def onInitialHcdShutdownComplete(): Unit
  def onRunningHcdShutdownComplete(): Unit
  def onLifecycle(message: ToComponentLifecycleMessage): Unit
  def onSetup(sc: Setup): Unit
  def onDomainMsg(msg: Msg): Unit

  init

  protected def init: Future[Unit] = async {
    await(initialize())
    mode = Initialized(ctx.self, pubSubRef)
    supervisor ! Initialized(ctx.self, pubSubRef)
  }

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (mode, msg) match {
      case (_: Initialized, x: InitialHcdMsg) ⇒ handleInitial(x)
      case (_: Running, x: RunningHcdMsg)     ⇒ handleRunning(x)
      case _                                  ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  private def handleInitial(x: InitialHcdMsg): Unit = x match {
    case Run(replyTo) =>
      onInitialRun()
      mode = Running(ctx.self, pubSubRef)
      replyTo ! Running(ctx.self, pubSubRef)
    case HcdShutdownComplete =>
      onInitialHcdShutdownComplete()
  }

  private def handleRunning(x: RunningHcdMsg): Unit = x match {
    case HcdShutdownComplete  => onRunningHcdShutdownComplete()
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }
}
