/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons

import org.apache.pekko.stream.{ActorAttributes, Attributes, Supervision}
import csw.event.api.exceptions.EventServerNotAvailable

/**
 * Use this decider for Event streams to stop the stream in case underlying server is not available and resume
 * in all other cases of exceptions
 */
private[event] object EventStreamSupervisionStrategy {
  private val decider: Supervision.Decider = {
    case _: EventServerNotAvailable => Supervision.Stop
    case _                          => Supervision.Resume
  }

  val attributes: Attributes = ActorAttributes.supervisionStrategy(decider)
}
