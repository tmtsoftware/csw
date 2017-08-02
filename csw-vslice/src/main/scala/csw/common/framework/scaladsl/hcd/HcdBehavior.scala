package csw.common.framework.scaladsl.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.HcdMsg.Submit
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehavior

import scala.reflect.ClassTag

class HcdBehavior[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg],
                                              supervisor: ActorRef[FromComponentLifecycleMessage],
                                              hcdHandlers: HcdHandlers[Msg])
    extends ComponentBehavior[Msg, HcdMsg](ctx, supervisor, hcdHandlers) {

  override def onRunningCompCommandMsg(x: HcdMsg): Unit = x match {
    case Submit(a) ⇒ hcdHandlers.onSetup(a)
  }
}
