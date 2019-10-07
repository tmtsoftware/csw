package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.command.client.messages.TopLevelActorMessage
import CommandComponentState._
import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.util.Timeout
import csw.command.api.CompleterActor
import csw.command.api.Completer.{Completer, OverallFailure, OverallResponse, OverallSuccess}
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.{ComponentId, TrackingEvent}
import csw.location.models.ComponentType.HCD
import csw.location.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import csw.command.api.CompleterActor.CommandCompleterMessage
import csw.command.api.CompleterActor.CommandCompleterMessage.{Update, WaitComplete}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class CommandAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._

  private val log: Logger                   = loggerFactory.getLogger(ctx)
  private implicit val ec: ExecutionContext = ctx.executionContext
  private val clientMat: Materializer       = ActorMaterializer()(ctx.system)
  private implicit val timeout: Timeout     = 15.seconds

  private val filterHCDConnection      = AkkaConnection(ComponentId("FilterHCD", HCD))
  val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(ctx.system, clientMat)

  private val filterHCDLocation    = Await.result(locationService.resolve(filterHCDConnection, 5.seconds), 5.seconds)
  var hcdComponent: CommandService = CommandServiceFactory.make(filterHCDLocation.get)(ctx.system)

  private val longRunning       = Setup(seqPrefix, longRunningCmdToHcd, None)
  private val shortRunning      = Setup(seqPrefix, shorterHcdCmd, None)
  private val shortRunningError = Setup(seqPrefix, shorterHcdErrorCmd, None)

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    println("Initializing Component TLA")
    Thread.sleep(100)
    //#currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    //#currentStatePublisher
    Future.unit
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command.commandName match {
      case `immediateCmd` | `longRunningCmd` | `longRunningCmdToAsm` | `longRunningCmdToAsmComp` | `longRunningCmdToAsmInvalid` |
          `longRunningCmdToAsmCActor` | `cmdWithBigParameter` =>
        Accepted(command.commandName, runId)
      case `invalidCmd` =>
        Invalid(command.commandName, runId, OtherIssue("Invalid"))
      case _ =>
        Invalid(command.commandName, runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(runId: Id, command: ControlCommand): SubmitResponse =
    command.commandName match {
      case `immediateCmd` =>
        Completed(command.commandName, runId)
      case `longRunningCmd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(2))) {
          // After time expires, send final update
          //commandUpdatePublisher.update(Completed(command.commandName, runId))
          commandResponseManager.updateCommand(Completed(command.commandName, runId))
        }
        // Starts long-runing and returns started
        Started(command.commandName, runId)
      case `longRunningCmdToAsm` =>
        hcdComponent.submitAndWait(longRunning).map {
          case r: Completed =>
            assert(r.commandName == longRunning.commandName)
            commandResponseManager.updateCommand(Completed(command.commandName, runId))
          case x =>
          //println("Some other response in asm: " + x)
        }
        // Assembly starts long-running and returns started
        Started(command.commandName, runId)
      case `longRunningCmdToAsmComp` =>
        val long    = hcdComponent.submit(longRunning)
        val shorter = hcdComponent.submit(shortRunning)
        Future.sequence(Set(long, shorter)).onComplete {
          case Success(responses) =>
            val completer: Completer = Completer(responses)
            responses.foreach(doComplete(_, completer))

            completer.waitComplete().collect {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(command.commandName, runId))
              case OverallFailure(responses) =>
                commandResponseManager.updateCommand(Error(command.commandName, runId, s"$responses"))
            }
          case Failure(exception) =>
            // Lift subcommand timeout to an error
            commandResponseManager.updateCommand(Error(command.commandName, runId, s"$exception"))
        }
        Started(command.commandName, runId)
      case `longRunningCmdToAsmCActor` =>
        implicit val timeout: Timeout     = 3.seconds
        implicit val scheduler: Scheduler = ctx.system.scheduler
        implicit val ec: ExecutionContext = ctx.system.executionContext

        val long    = hcdComponent.submit(longRunning)
        val shorter = hcdComponent.submit(shortRunning)
        Future.sequence(Set(long, shorter)).onComplete {
          case Success(responses) =>
            val completer: ActorRef[CommandCompleterMessage] = ctx.spawn(CompleterActor(responses), "c1")
            responses.foreach(doComplete2(_, completer))

            val f: Future[OverallResponse] = completer.ask(ref => WaitComplete(ref))
            f.map {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(command.commandName, runId))
              case OverallFailure(responses) =>
                commandResponseManager.updateCommand(Error(command.commandName, runId, s"$responses"))
            }

          case Failure(exception) =>
            // Lift subcommand timeout to an error
            commandResponseManager.updateCommand(Error(command.commandName, runId, s"$exception"))
        }
        Started(command.commandName, runId)
      case `longRunningCmdToAsmInvalid` =>
        val long    = hcdComponent.submit(longRunning)
        val shorter = hcdComponent.submit(shortRunningError)
        Future.sequence(Set(long, shorter)).onComplete {
          case Success(responses) =>
            val completer = Completer(responses)
            responses.foreach(doComplete(_, completer))

            completer.waitComplete().collect {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(command.commandName, runId))
              case OverallFailure(_) =>
                // Could look at the responses here and improve update
                commandResponseManager.updateCommand(Error(command.commandName, runId, "ERROR"))
            }
          case Failure(exception) =>
            // Lift subcommand timeout to an error
            commandResponseManager.updateCommand(Error(command.commandName, runId, s"$exception"))
        }
        Started(command.commandName, runId)
      case `cmdWithBigParameter` =>
        Completed(command.commandName, runId, Result(command.paramSet))
      case _ =>
        Invalid(command.commandName, runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }

  private def doComplete(firstResponse: SubmitResponse, completer: Completer): Unit = {
    firstResponse match {
      case Started(_, runId) => hcdComponent.queryFinal(runId).map(completer.update)
      case a =>
        completer.update(Error(a.commandName, a.runId, "First command response was not started!"))
    }
  }

  private def doComplete2(firstResponse: SubmitResponse, completer: ActorRef[CommandCompleterMessage]): Unit = {
    firstResponse match {
      case Started(_, runId) => hcdComponent.queryFinal(runId).map(completer ! Update(_))
      case a =>
        completer ! Update(Error(a.commandName, a.runId, "First command response was not started!"))
    }
  }

  override def onShutdown(): Future[Unit] = Future {
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
