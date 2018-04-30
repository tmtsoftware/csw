package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.states.CurrentState
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class InitFailureComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      loggerFactory: LoggerFactory
    ) {

  val log: Logger                   = new LoggerFactory(componentInfo.name).getLogger(ctx)
  implicit val ec: ExecutionContext = ctx.executionContext

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    if (testRestart) {
      testRestart = false
      throw TestFailureStop("testing FailureStop")
    }
    testRestart = true
    Future {
      // small sleep is required in order for test probe to subscribe for component state and lifecycle state
      // before component actually gets initialized
      Thread.sleep(200)
      currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    }

  }

  override def onGoOffline(): Unit = currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit = currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

  override def onSubmit(controlCommand: ControlCommand): Unit = {}

  override def onOneway(controlCommand: ControlCommand): Unit = {}

  def validateCommand(command: ControlCommand): CommandResponse = Accepted(command.runId)

  override def onShutdown(): Future[Unit] = {
    Future.successful(currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}
}
