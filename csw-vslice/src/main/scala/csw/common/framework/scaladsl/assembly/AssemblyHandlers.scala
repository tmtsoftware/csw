package csw.common.framework.scaladsl.assembly

import akka.typed.scaladsl.ActorContext
import csw.common.ccs.Validation.Validation
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{AssemblyMsg, ComponentMsg}
import csw.common.framework.scaladsl.LifecycleHandlers

import scala.reflect.ClassTag

abstract class AssemblyHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg], info: AssemblyInfo)
    extends LifecycleHandlers[Msg] {

  def onControlCommand(assemblyMsg: AssemblyMsg): Validation
}
