package csw.trombone.assembly.actors

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation
import csw.common.ccs.Validation.{Valid, Validation}
import csw.common.framework.models.Component.{AssemblyInfo, ComponentInfo}
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.SupervisorIdleMsg.Running
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.commands.{Observe, Setup}
import csw.param.states.CurrentState
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.DiagPublisherMessages.{DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly.TromboneCommandHandlerMsgs.NotFollowingMsgs
import csw.trombone.assembly._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class TromboneAssemblyBehaviorFactory extends ComponentBehaviorFactory[DiagPublisherMessages] {
  override def make(ctx: ActorContext[ComponentMsg],
                    componentInfo: ComponentInfo,
                    pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[DiagPublisherMessages] =
    new TromboneAssemblyHandlers(ctx, componentInfo)
}

class TromboneAssemblyHandlers(ctx: ActorContext[ComponentMsg], componentInfo: ComponentInfo)
    extends ComponentHandlers[DiagPublisherMessages](ctx, componentInfo) {

  private var diagPublsher: ActorRef[DiagPublisherMessages] = _

  private var commandHandler: ActorRef[NotFollowingMsgs] = _

  implicit var ac: AssemblyContext  = _
  implicit val ec: ExecutionContext = ctx.executionContext

  val runningHcd: Option[Running] = None

  def onRun(): Unit = ()

  def initialize(): Future[Unit] = async {
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(componentInfo.asInstanceOf[AssemblyInfo], calculationConfig, controlConfig)

    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    commandHandler = ctx.spawnAnonymous(TromboneCommandHandler.make(ac, runningHcd, Some(eventPublisher)))

    diagPublsher = ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcd, Some(eventPublisher)))
  }

  override def onShutdown(): Unit = println("Received Shutdown")

  override def onRestart(): Unit = println("Received dorestart")

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received GoOnline")

  def onDomainMsg(mode: DiagPublisherMessages): Unit = mode match {
    case (DiagnosticState | OperationsState) => diagPublsher ! mode
    case _                                   ⇒
  }

  override def onControlCommand(commandMsg: CommandMsg): Validation = commandMsg.command match {
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

  private def observe(o: Observe, replyTo: ActorRef[CommandResponse]): Validation = Validation.Valid

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = ???
}
