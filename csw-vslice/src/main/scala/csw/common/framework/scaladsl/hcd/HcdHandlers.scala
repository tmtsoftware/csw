package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.LifecycleHandlers
import csw.param.Parameters.Setup

import scala.reflect.ClassTag

abstract class HcdHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo)
    extends LifecycleHandlers[Msg] {

  def onSetup(sc: Setup): Unit
}
