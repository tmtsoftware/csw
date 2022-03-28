/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.api.messages

import akka.util.Timeout
import csw.params.core.models.Id
import csw.params.core.states.StateName

sealed trait CommandServiceStreamRequest

object CommandServiceStreamRequest {
  case class QueryFinal(runId: Id, timeoutInSeconds: Timeout)         extends CommandServiceStreamRequest
  case class SubscribeCurrentState(names: Set[StateName] = Set.empty) extends CommandServiceStreamRequest
}
