package csw.common.framework.javadsl.hcd

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models._
import csw.common.framework.scaladsl.hcd.HcdHandlers

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class JHcdHandlers[Msg <: DomainMsg](ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo, klass: Class[Msg])
    extends HcdHandlers[Msg](ctx.asScala, hcdInfo)(ClassTag(klass)) {

  def jInitialize(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ ())
}
