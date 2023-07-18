/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.common.components.framework

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.*
import csw.params.commands.CommandResponse.*
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

class InvalidComponentHandlers(cswCtx: CswContext, ctx: ActorContext[TopLevelActorMessage])
    extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx.*

  val log: Logger = loggerFactory.getLogger(ctx)

  override def initialize(): Unit = {
    log.info("Initializing Component TLA")
  }

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = ???

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Unit = ???

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = ???
}
