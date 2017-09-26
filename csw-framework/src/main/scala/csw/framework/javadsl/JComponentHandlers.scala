package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.framework.models._
import csw.framework.scaladsl.ComponentHandlers
import csw.param.messages.ComponentMessage
import csw.param.messages.PubSub.PublisherMessage
import csw.param.messages.RunningMessage.DomainMessage
import csw.param.states.CurrentState
import csw.services.location.javadsl.ILocationService

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

abstract class JComponentHandlers[Msg <: DomainMessage](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: ILocationService,
    klass: Class[Msg]
) extends ComponentHandlers[Msg](ctx.asScala, componentInfo, pubSubRef, locationService.asScala)(ClassTag(klass)) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  def jInitialize(): CompletableFuture[Unit]
  def jOnRun(): CompletableFuture[Unit]
  def jOnShutdown(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala
  override def onRun(): Future[Unit]      = jOnRun().toScala
  override def onShutdown(): Future[Unit] = jOnShutdown().toScala
}
