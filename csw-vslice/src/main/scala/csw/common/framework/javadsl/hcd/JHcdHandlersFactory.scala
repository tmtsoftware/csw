package csw.common.framework.javadsl.hcd

import akka.typed.javadsl.ActorContext
import csw.common.framework.models.{DomainMsg, HcdMsg}

abstract class JHcdHandlersFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[HcdMsg]): JHcdHandlers[Msg]
}
