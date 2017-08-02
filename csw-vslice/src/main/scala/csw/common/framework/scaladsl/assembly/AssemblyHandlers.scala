package csw.common.framework.scaladsl.assembly

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation.Validation
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.ComponentMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.scaladsl.LifecycleHandlers
import csw.param.Parameters.{Observe, Setup}

import scala.reflect.ClassTag

abstract class AssemblyHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg], info: AssemblyInfo)
    extends LifecycleHandlers[Msg] {
  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation
  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation
}
