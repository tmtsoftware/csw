package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ComponentMessage
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
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
  def jOnShutdown(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala
  override def onShutdown(): Future[Unit] = jOnShutdown().toScala
}
