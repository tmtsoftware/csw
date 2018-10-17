package csw.common.utils

import akka.actor.typed.ActorRef
import csw.command.client.models.framework.LockingResponse
import csw.params.core.models.Prefix
import csw.command.client.messages.SupervisorLockMessage.Lock

import scala.concurrent.duration.DurationLong

object LockCommandFactory {
  def make(prefix: Prefix, replyTo: ActorRef[LockingResponse]) = Lock(prefix, replyTo, 10.seconds)
}
