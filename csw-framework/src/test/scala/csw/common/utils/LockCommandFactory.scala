/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.common.utils

import org.apache.pekko.actor.typed.ActorRef
import csw.command.client.models.framework.LockingResponse
import csw.command.client.messages.SupervisorLockMessage.Lock
import csw.prefix.models.Prefix

import scala.concurrent.duration.DurationLong

object LockCommandFactory {
  def make(prefix: Prefix, replyTo: ActorRef[LockingResponse]) = Lock(prefix, replyTo, 10.seconds)
}
