package csw.framework.components.hcd

import java.nio.file.Paths

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.{Cancellable, Scheduler}
import akka.util.Timeout
import csw.framework.components.ConfigNotAvailableException
import csw.framework.components.assembly.WorkerActorMsgs.{GetStatistics, InitialState}
import csw.framework.components.assembly.{WorkerActor, WorkerActorMsg}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.Responses.{Accepted, Completed, SubmitResponse, ValidationResponse}
import csw.messages.commands._
import csw.messages.events._
import csw.messages.framework.ComponentInfo
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.services.config.api.models.ConfigData
import csw.services.logging.scaladsl.Logger
import csw.command.messages.TopLevelActorMessage
import csw.params.commands.CommandResponse.Accepted
import csw.params.commands.{CommandResponse, ControlCommand, Observe, Setup}
import csw.location.api.models.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.config.api.models.ConfigData
import csw.logging.scaladsl.Logger

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

//#component-handlers-class
class HcdComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx)
//#component-handlers-class
    {

  import cswCtx._

  val log: Logger = loggerFactory.getLogger(ctx)

  implicit val ec: ExecutionContext = ctx.executionContext
  implicit val timeout: Timeout     = 5.seconds
  implicit val scheduler: Scheduler = ctx.system.scheduler
  var current: Int                  = _
  var stats: Int                    = _

  //#initialize-handler
  override def initialize(): Future[Unit] = async {

    // fetch config (preferably from configuration service)
    val hcdConfig = await(getHcdConfig)

    // create a worker actor which is used by this hcd
    val worker: ActorRef[WorkerActorMsg] = ctx.spawnAnonymous(WorkerActor.make(hcdConfig))

    // initialise some state by using the worker actor created above
    current = await(worker ? InitialState)
    stats = await(worker ? GetStatistics)

  }
  //#initialize-handler

  //#validateCommand-handler
  override def validateCommand(controlCommand: ControlCommand): ValidationResponse = controlCommand match {
    case _: Setup   ⇒ Accepted(controlCommand.runId) // validation for setup goes here
    case _: Observe ⇒ Accepted(controlCommand.runId) // validation for observe goes here
  }
  //#validateCommand-handler

  //#onSubmit-handler
  override def onSubmit(controlCommand: ControlCommand): SubmitResponse = controlCommand match {
    case setup: Setup     ⇒ submitSetup(setup) // includes logic to handle Submit with Setup config command
    case observe: Observe ⇒ submitObserve(observe) // includes logic to handle Submit with Observe config command
  }
  //#onSubmit-handler

  //#onOneway-handler
  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand match {
    case setup: Setup     ⇒ onewaySetup(setup) // includes logic to handle Oneway with Setup config command
    case observe: Observe ⇒ onewayObserve(observe) // includes logic to handle Oneway with Setup config command
  }
  //#onOneway-handler

  //#onGoOffline-handler
  override def onGoOffline(): Unit = {
    // do something when going offline
  }
  //#onGoOffline-handler

  //#onGoOnline-handler
  override def onGoOnline(): Unit = {
    // do something when going online
  }
  //#onGoOnline-handler

  //#onShutdown-handler
  override def onShutdown(): Future[Unit] = async {
    // clean up resources
  }
  //#onShutdown-handler

  //#onLocationTrackingEvent-handler
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location)   ⇒ // do something for the tracked location when it is updated
    case LocationRemoved(connection) ⇒ // do something for the tracked location when it is no longer available
  }
  //#onLocationTrackingEvent-handler

  private def processSetup(sc: Setup): Unit = {
    sc.commandName.toString match {
      case "axisMove"   ⇒
      case "axisDatum"  ⇒
      case "axisHome"   ⇒
      case "axisCancel" ⇒
      case x            ⇒ log.error(s"Invalid command [$x] received.")
    }
  }

  private def processObserve(oc: Observe): Unit = {
    oc.commandName.toString match {
      case "point"   ⇒
      case "acquire" ⇒
      case x         ⇒ log.error(s"Invalid command [$x] received.")
    }
  }

  /**
   * in case of submit command, component writer is required to update commandResponseManager with the result
   */
  private def submitSetup(setup: Setup): SubmitResponse = {
    processSetup(setup)
    Completed(setup.runId)
  }

  private def submitObserve(observe: Observe): SubmitResponse = {
    processObserve(observe)
    Completed(observe.runId)
  }

  private def onewaySetup(setup: Setup): Unit = processSetup(setup)

  private def onewayObserve(observe: Observe): Unit = processObserve(observe)

  private def getHcdConfig: Future[ConfigData] = {

    configClientService.getActive(Paths.get("tromboneAssemblyContext.conf")).flatMap {
      case Some(config) ⇒ Future.successful(config) // do work
      case None         ⇒
        // required configuration could not be found in the configuration service. Component can choose to stop until the configuration is made available in the
        // configuration service and started again
        throw ConfigNotAvailableException()
    }
  }

}
