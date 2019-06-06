package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.command.client.messages.TopLevelActorMessage
import csw.command.client.models.framework.PubSub.Publish
import CommandComponentState._
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, TrackingEvent}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import scala.async.Async._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommandAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  val log: Logger                     = loggerFactory.getLogger(ctx)
  implicit val ec: ExecutionContext   = ctx.executionContext
  private val clientMat: Materializer = ActorMaterializer()(ctx.system)
  implicit val timeout: Timeout       = 15.seconds

  private val filterHCDConnection = AkkaConnection(ComponentId("FilterHCD", HCD))
  //val seedLocationService: LocationService                 = HttpLocationServiceFactory.makeLocalClient(ctx.system, clientMat)

  //val filterHCDLocation  = Await.result(seedLocationService.find(filterHCDConnection), 5.seconds)
  var hcdComponent: CommandService = _

  //val filterCommandService:CommandService = CommandServiceFactory.make(filterHCDLocation.get)(ctx.system)

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)

    println("Initialize Assembly")

    cswCtx.locationService.resolve(filterHCDConnection, 5.seconds).map {
      case Some(akkaLocation) ⇒ hcdComponent = CommandServiceFactory.make(akkaLocation)(ctx.system)
      case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
    }
    //#currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    //currentStatePublisher.publish(CurrentState(filterPrefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    //#currentStatePublisher

    Future.unit
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(filterPrefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(filterPrefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command.commandName match {
      case `immediateCmd` =>
        Accepted(command.commandName, runId)
      case `longRunningCmd` =>
        Accepted(command.commandName, runId)
      case `longRunningCmdToHcd` =>
        println(s"Long runingCDMtoHCD in Assembly: $runId")
        Accepted(command.commandName, runId)
      case `invalidCmd` =>
        Invalid(command.commandName, runId, OtherIssue("Invalid"))
      case _ =>
        Invalid(command.commandName, runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    println("SUBMIT SUBMIT")
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(runId: Id, command: ControlCommand): SubmitResponse = {
    println("PROCESS COMMAND")
    command.commandName match {
      case `immediateCmd` =>
        Completed(command.commandName, runId)
      case `longRunningCmd` =>
        //commandUpdatePublisher.update(Completed(controlCommand.commandName, runId))
        ctx.scheduleOnce(
          3.seconds,
          commandUpdatePublisher.publisherActor,
          Publish[SubmitResponse](Completed(command.commandName, runId))
        )
        Started(command.commandName, runId)
      case `longRunningCmdToHcd` =>
        println("Long runingCDMtoHCD in Assembly")
        //async {
        hcdComponent.submitAndWait(command).map {
          case completed: Completed =>
            println(s"Submit wait finished: $completed")
            commandUpdatePublisher.update(completed)
          case x =>
            println("Soem other response: " + x)
        }

        //timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now.value.plusSeconds(2))) {
//          println("Timer")
        //        commandUpdatePublisher.update(Completed(command.commandName, runId))
        //    }
        println("Assembly returning started")
        Started(command.commandName, runId)
      //case Setup(somePrefix, CommandName("subscribe.event.success"), _, _) ⇒
//        eventService.defaultSubscriber.subscribeCallback(Set(event.eventKey), processEvent(somePrefix))
      /*
      case Setup(_, CommandName("time.service.scheduler.success"), _, _) =>
        timeServiceScheduler.scheduleOnce(UTCTime.now()) {
          currentStatePublisher.publish(CurrentState(filterPrefix, timeServiceSchedulerState))
        }

      case Setup(somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(setupConfigChoice))
        )

      case Observe(somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(observeConfigChoice))
        )

       */
      case _ ⇒
        Invalid(command.commandName, runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }
  }

  override def onShutdown(): Future[Unit] = Future {
    //currentStatePublisher.publish(CurrentState(filterPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???
}
