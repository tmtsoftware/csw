package csw.common.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{javadsl, ActorRef, Behavior}
import csw.common.framework.models.{DomainMsg, HcdComponentLifecycleMessage, HcdMsg, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.HcdActor
import csw.param.Parameters

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future
import scala.reflect.ClassTag

object JClassTag {
  def make[T](klass: Class[T]): ClassTag[T] = ClassTag(klass)
}

abstract class JHcdActorFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: javadsl.ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage]): JHcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx.asJava, supervisor)).narrow
}

abstract class JHcdActor[Msg <: DomainMsg](ctx: ActorContext[HcdMsg],
                                           supervisor: ActorRef[HcdComponentLifecycleMessage],
                                           klass: Class[Msg])
    extends HcdActor[Msg](ctx.asScala, supervisor)(ClassTag(klass)) {

  def jInitialize(): CompletableFuture[Void]
  def jOnRun(): Void
  def jOnShutdown(): Void
  def jOnShutdownComplete(): Void
  def jOnLifecycle(x: ToComponentLifecycleMessage): Void
  def jOnSetup(sc: Parameters.Setup): Void
  def jOnDomainMsg(msg: Msg): Void

  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ Unit)

  override def onRun(): Unit = jOnRun()

  override def onShutdown(): Unit = jOnShutdown()

  override def onShutdownComplete(): Unit = jOnShutdownComplete()

  override def onLifecycle(x: ToComponentLifecycleMessage): Unit = jOnLifecycle(x)

  override def onSetup(sc: Parameters.Setup): Unit = jOnSetup(sc)

  override def onDomainMsg(msg: Msg): Unit = jOnDomainMsg(msg)
}
