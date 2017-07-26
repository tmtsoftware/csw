package csw.common.components.hcd

import akka.typed.ActorRef
import csw.common.framework.models.DomainMsg

sealed trait HcdDomainMessage extends DomainMsg

case class GetCurrentState(replyTo: ActorRef[HcdDomainResponseMsg]) extends HcdDomainMessage

sealed trait HcdDomainResponseMsg

case object ReceivedRunOffline extends HcdDomainResponseMsg
case object ReceivedRestart    extends HcdDomainResponseMsg

sealed trait LifecycleMessageReceived
object LifecycleMessageReceived {
  case object Restart              extends LifecycleMessageReceived
  case object RunOffline           extends LifecycleMessageReceived
  case object RunOnline            extends LifecycleMessageReceived
  case object Shutdown             extends LifecycleMessageReceived
  case object LifecyclefailureInfo extends LifecycleMessageReceived
}
