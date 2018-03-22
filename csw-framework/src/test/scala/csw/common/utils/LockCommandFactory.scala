package csw.common.utils

import akka.actor.typed.ActorRef
import csw.messages.framework.LockingResponse
import csw.messages.params.models.Prefix
import csw.messages.scaladsl.SupervisorLockMessage.Lock

import scala.concurrent.duration.DurationLong

object LockCommandFactory {
  def make(prefix: Prefix, replyTo: ActorRef[LockingResponse]) = Lock(prefix, replyTo, 10.seconds)
}
