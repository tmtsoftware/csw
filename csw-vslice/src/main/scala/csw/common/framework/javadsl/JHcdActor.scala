package csw.common.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{javadsl, ActorRef, Behavior}
import csw.common.framework.models._
import csw.common.framework.scaladsl.HcdActor
import csw.param.Parameters

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future
import scala.reflect.ClassTag

object JClassTag {
  def make[T](klass: Class[T]): ClassTag[T] = ClassTag(klass)
}

abstract class JHcdActorFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: javadsl.ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]): JHcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx.asJava, supervisor)).narrow
}

abstract class JHcdActor[Msg <: DomainMsg](ctx: ActorContext[HcdMsg],
                                           supervisor: ActorRef[HcdResponseMode],
                                           klass: Class[Msg])
    extends HcdActor[Msg](ctx.asScala, supervisor)(ClassTag(klass)) {

  def jInitialize(): CompletableFuture[Void]
  def jOnInitialRun(): Void
  def jOnRunningHcdShutdownComplete(): Void
  def jOnSetup(sc: Parameters.Setup): Void
  def jOnDomainMsg(msg: Msg): Void
  def jOnInitialHcdShutdownComplete(): Void
  def jOnShutdown(): Void
  def jOnRestart(): Void
  def jOnRun(): Void
  def jOnRunOffline(): Void
  def jOnLifecycleFailureInfo(state: LifecycleState, reason: String): Void
  def jOnShutdownComplete(): Void

  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ Unit)

  override def onInitialRun(): Unit = jOnInitialRun()

  override def onRunningHcdShutdownComplete(): Unit = jOnRunningHcdShutdownComplete()

  override def onSetup(sc: Parameters.Setup): Unit = jOnSetup(sc)

  override def onDomainMsg(msg: Msg): Unit = jOnDomainMsg(msg)

  override def onInitialHcdShutdownComplete(): Unit = jOnInitialHcdShutdownComplete()

  override def onShutdown(): Unit = jOnShutdown()

  override def onRestart(): Unit = jOnRestart()

  override def onRunOnline(): Unit = jOnRun()

  override def onRunOffline(): Unit = jOnRunOffline()

  override def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit =
    jOnLifecycleFailureInfo(state, reason)

  override def onShutdownComplete(): Unit = jOnShutdownComplete()
}
