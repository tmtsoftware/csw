/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.api.messages

import csw.params.commands.ControlCommand
import csw.params.core.models.Id

sealed trait CommandServiceRequest

object CommandServiceRequest {
  case class Validate(controlCommand: ControlCommand) extends CommandServiceRequest
  case class Submit(controlCommand: ControlCommand)   extends CommandServiceRequest
  case class Oneway(controlCommand: ControlCommand)   extends CommandServiceRequest
  case class Query(runId: Id)                         extends CommandServiceRequest
}
