package org.tmt.nfiraos.sampleassembly

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.{ObsId, Units}
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.{CommandResponseManager, CommandService}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class SampleAssemblyHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory) {

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  //#worker-actor
  sealed trait WorkerCommand
  case class SendCommand(hcd: CommandService) extends WorkerCommand

  private val commandSender =
    ctx.spawn(
      Behaviors.receiveMessage[WorkerCommand](msg => {
        msg match {
          case command: SendCommand =>
            log.trace(s"WorkerActor received SendCommand message.")
            handle(command.hcd)
          case _ => log.error("Unsupported message type")
        }
        Behaviors.same
      }),
      "CommandSender"
    )

  private implicit val submitTimeout: Timeout = Timeout(1000.millis)
  def handle(hcd: CommandService): Unit = {

    // Construct Setup command
    val sleepTimeKey: Key[Long]         = KeyType.LongKey.make("SleepTime")
    val sleepTimeParam: Parameter[Long] = sleepTimeKey.set(5000).withUnits(Units.millisecond)
    val setupCommand                    = Setup(componentInfo.prefix, CommandName("sleep"), Some(ObsId("2018A-001"))).add(sleepTimeParam)

    // Submit command, and handle validation response. Final response is returned as a Future
    val submitCommandResponseF: Future[CommandResponse] = hcd.submit(setupCommand).flatMap {
      case _: Accepted =>
        // If valid, subscribe to the HCD's CommandResponseManager
        // This explicit timeout indicates how long to wait for completion
        hcd.subscribe(setupCommand.runId)(10000.seconds)
      case x =>
        log.error("Sleep command invalid")
        Future(x)
    }

    // Wait for final response, and log result
    submitCommandResponseF.foreach {
      case _: CommandResponse.Completed => log.info("Command completed successfully")
      case x: CommandResponse.Error     => log.error(s"Command Completed with error: ${x.message}")
      case _                            => log.error("Command failed")
    }
  }
  //#worker-actor

  //#initialize
  override def initialize(): Future[Unit] = {
    log.info("In Assembly initialize")
    Future.unit
  }

  override def onShutdown(): Future[Unit] = {
    log.info("Assembly is shutting down.")
    Future.unit
  }
  //#initialize

  //#track-location
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        val hcd = new CommandService(location.asInstanceOf[AkkaLocation])(ctx.system)
        commandSender ! SendCommand(hcd)
      case LocationRemoved(_) => log.info("HCD no longer available")
    }
  }
  //#track-location

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = ???

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

}
