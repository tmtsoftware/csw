package org.tmt.nfiraos.samplehcd

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.Id
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * These handlers get invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd, these will be first validated in the
 * supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class SampleHcdHandlers(
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
  case class Sleep(runId: Id, timeInMillis: Long) extends WorkerCommand

  private val workerActor =
    ctx.spawn(
      Behaviors.receiveMessage[WorkerCommand](msg => {
        msg match {
          case sleep: Sleep =>
            log.trace(s"WorkerActor received sleep command with time of ${sleep.timeInMillis} ms")
            // simulate long running command
            Thread.sleep(sleep.timeInMillis)
            commandResponseManager.addOrUpdateCommand(sleep.runId, CommandResponse.Completed(sleep.runId))
          case _ => log.error("Unsupported message type")
        }
        Behaviors.same
      }),
      "WorkerActor"
    )
  //#worker-actor

  //#initialize
  override def initialize(): Future[Unit] = {
    log.info("In HCD initialize")
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"TrackingEvent received: ${trackingEvent.connection.name}")
  }

  override def onShutdown(): Future[Unit] = {
    log.info("HCD is shutting down")
    Future.unit
  }
  //#initialize

  //#validate
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    log.info(s"Validating command: ${controlCommand.commandName.name}")
    controlCommand.commandName.name match {
      case "sleep" => CommandResponse.Accepted(controlCommand.runId)
      case x       => CommandResponse.Invalid(controlCommand.runId, CommandIssue.UnsupportedCommandIssue(s"Command $x. not supported."))
    }
  }
  //#validate

  //#onSetup
  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.info(s"Handling command: ${controlCommand.commandName}")

    controlCommand match {
      case setupCommand: Setup     => onSetup(setupCommand)
      case observeCommand: Observe => // implement (or not)
    }
  }

  def onSetup(setup: Setup): Unit = {
    val sleepTimeKey: Key[Long] = KeyType.LongKey.make("SleepTime")

    // get param from the Parameter Set in the Setup
    val sleepTimeParam: Parameter[Long] = setup(sleepTimeKey)

    // values of parameters are arrays. Get the first one (the only one in our case) using `head` method available as a convenience method on `Parameter`.
    val sleepTimeInMillis: Long = sleepTimeParam.head

    log.info(s"command payload: ${sleepTimeParam.keyName} = $sleepTimeInMillis")

    workerActor ! Sleep(setup.runId, sleepTimeInMillis)
  }
  //#onSetup

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

}
