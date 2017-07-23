package csw.common.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.common.framework.models.{DomainMsg, HcdComponentLifecycleMessage, HcdMsg, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.HcdActor
import csw.param.Parameters

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

abstract class JHcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg],
                                                     supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[Msg](ctx.asScala, supervisor) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

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
