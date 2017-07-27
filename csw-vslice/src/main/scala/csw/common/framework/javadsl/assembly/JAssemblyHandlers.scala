package csw.common.framework.javadsl.assembly

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models._
import csw.common.framework.scaladsl.assembly.AssemblyHandlers

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class JAssemblyHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg], assemblyInfo: AssemblyInfo)
    extends AssemblyHandlers[Msg](ctx.asScala, assemblyInfo) {

  def jInitialize(): CompletableFuture[Unit]

  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ ())
}
