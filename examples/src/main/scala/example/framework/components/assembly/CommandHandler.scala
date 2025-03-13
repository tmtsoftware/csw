/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework.components.assembly

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import csw.command.api.scaladsl.CommandService
import csw.config.api.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def behavior(configData: ConfigData, runningIn: Option[CommandService]): Behavior[CommandHandlerMsgs] =
    Behaviors.setup { ctx =>
      // setup required for actor

      Behaviors.receiveMessage { case _ => // Handle messages and return new behavior
        Behaviors.same
      }
    }
}
