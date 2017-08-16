package csw.common.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentHandlers
import csw.param.states.CurrentState

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

abstract class JComponentHandlers[Msg <: DomainMsg](
    ctx: ActorContext[ComponentMsg],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMsg[CurrentState]]
)(klass: Class[Msg])
    extends ComponentHandlers[Msg](ctx.asScala, componentInfo, pubSubRef)(ClassTag(klass)) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  def jInitialize(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala
}
