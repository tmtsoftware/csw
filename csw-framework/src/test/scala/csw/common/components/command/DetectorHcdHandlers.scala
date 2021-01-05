package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.CommandComponentState._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{IRDetectorEvent, OpticalDetectorEvent, WFSDetectorEvent}
import csw.time.core.models.UTCTime

class DetectorHcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  private val log: Logger = loggerFactory.getLogger(ctx)

  override def initialize(): Unit = {
    log.info("Initializing HCD Component TLA")
    //#CSW-118 : publishing observe events for IR, Optical & WFS detectors
    val obsId      = ObsId("scala_obs_id")
    val exposureId = "some_exposure_id"

    val observeStart   = IRDetectorEvent.ObserveStart.create(filterHcdPrefix.toString, obsId)
    val exposureStart  = OpticalDetectorEvent.ExposureStart.create(filterHcdPrefix.toString, obsId, exposureId)
    val publishSuccess = WFSDetectorEvent.PublishSuccess.create(filterHcdPrefix.toString)
    eventService.defaultPublisher.publish(observeStart)
    eventService.defaultPublisher.publish(exposureStart)
    eventService.defaultPublisher.publish(publishSuccess)
    //#CSW-118
  }

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = ???
}
