package csw.trombone.assembly.actors

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.models.ComponentInfo
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.commands.{Observe, Setup}
import csw.param.messages.FromComponentLifecycleMessage.Running
import csw.param.messages.PubSub.PublisherMessage
import csw.param.messages._
import csw.param.models.{Validation, Validations}
import csw.param.models.Validations.Valid
import csw.param.states.CurrentState
import csw.services.location.models.TrackingEvent
import csw.services.location.scaladsl.LocationService
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.DiagPublisherMessages.{DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly.TromboneCommandHandlerMsgs.NotFollowingMsgs
import csw.trombone.assembly._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class TromboneAssemblyBehaviorFactory extends ComponentBehaviorFactory[DiagPublisherMessages] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[DiagPublisherMessages] =
    new TromboneAssemblyHandlers(ctx, componentInfo, pubSubRef, locationService)
}

class TromboneAssemblyHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[DiagPublisherMessages](ctx, componentInfo, pubSubRef, locationService) {

  private var diagPublsher: ActorRef[DiagPublisherMessages] = _

  private var commandHandler: ActorRef[NotFollowingMsgs] = _

  implicit var ac: AssemblyContext  = _
  implicit val ec: ExecutionContext = ctx.executionContext

  val runningHcd: Option[Running] = None

  def onRun(): Future[Unit] = Future.unit

  def initialize(): Future[Unit] = async {
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(componentInfo.asInstanceOf[ComponentInfo], calculationConfig, controlConfig)

    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    commandHandler = ctx.spawnAnonymous(TromboneCommandHandler.make(ac, runningHcd, Some(eventPublisher)))

    diagPublsher = ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcd, Some(eventPublisher)))
  }

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("Received Shutdown"))
  }

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received GoOnline")

  def onDomainMsg(mode: DiagPublisherMessages): Unit = mode match {
    case (DiagnosticState | OperationsState) => diagPublsher ! mode
    case _                                   ⇒
  }

  override def onControlCommand(commandMsg: CommandMessage): Validation = commandMsg.command match {
    case x: Setup   => setup(x, commandMsg.replyTo)
    case x: Observe => observe(x, commandMsg.replyTo)
  }

  private def setup(s: Setup, commandOriginator: ActorRef[CommandResponse]): Validation = {
    val validation = validateOneSetup(s)
    if (validation == Valid) {
      commandHandler ! TromboneCommandHandlerMsgs.Submit(s, commandOriginator)
    }
    validation
  }

  private def observe(o: Observe, replyTo: ActorRef[CommandExecutionResponse]): Validation = Validations.Valid

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = ???

  override def onCommandValidationNotification(validationResponse: CommandValidationResponse): Unit = ???

  override def onCommandExecutionNotification(executionResponse: CommandExecutionResponse): Unit = ???

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???
}
