package csw.common.framework.javadsl.hcd

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.common.framework.models._
import csw.common.framework.scaladsl.hcd.HcdActor
import csw.param.Parameters

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future
import scala.reflect.ClassTag

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
