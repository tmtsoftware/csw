package csw.common.utils

import akka.typed.ActorRef
import csw.messages.scaladsl.SupervisorLockMessage.Lock
import csw.messages.framework.LockingResponse
import csw.messages.params.models.Prefix

import scala.concurrent.duration.DurationLong

object LockCommandFactory {
  def make(prefix: Prefix, replyTo: ActorRef[LockingResponse]) = Lock(prefix, replyTo, 10.seconds)
}
