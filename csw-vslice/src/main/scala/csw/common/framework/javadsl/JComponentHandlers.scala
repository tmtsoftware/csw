package csw.common.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentHandlers

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

abstract class JComponentHandlers[Msg <: DomainMsg: ClassTag](
    ctx: ActorContext[ComponentMsg],
    componentInfo: ComponentInfo
) extends ComponentHandlers[Msg](ctx.asScala, componentInfo) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  def jInitialize(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala
}
