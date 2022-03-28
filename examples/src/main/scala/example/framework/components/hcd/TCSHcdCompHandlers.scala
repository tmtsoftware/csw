/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework.components.hcd

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.DefaultComponentHandlers
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.duration.DurationLong

//#component-handlers-class
class TCSHcdCompHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends DefaultComponentHandlers(ctx, cswCtx) {
//#component-handlers-class

  /**
   * On receiving a command as Submit, the onSubmit handler is invoked for a component only if the validateCommand handler
   * returns Accepted. In case a command is received as a submit, command response should be updated in the CommandResponseManager.
   * CommandResponseManager is an actor whose reference commandResponseManager is available in the ComponentHandlers.
   *
   * @param runId          Run ID for command tracking
   * @param controlCommand represents a command received e.g. Setup, Observe or wait
   * @return response for command submission
   */
  override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
    controlCommand.commandName.toString match {
      case "move" =>
        cswCtx.timeServiceScheduler.scheduleOnce(UTCTime.after(5.seconds))(
          cswCtx.commandResponseManager.updateCommand(Completed(runId))
        )
        Started(runId)
      case _ => Completed(runId)
    }
  }
}
