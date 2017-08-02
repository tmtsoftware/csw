package csw.trombone.assembly.actors

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation
import csw.common.ccs.Validation.{Valid, Validation}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.SupervisorIdleMsg.Running
import csw.common.framework.models._
import csw.common.framework.scaladsl.assembly.{AssemblyHandlers, AssemblyHandlersFactory}
import csw.param.Parameters.{Observe, Setup}
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.DiagPublisherMessages.{DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly.TromboneCommandHandlerMsgs.NotFollowingMsgs
import csw.trombone.assembly._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class TromboneAssemblyHandlersFactory extends AssemblyHandlersFactory[DiagPublisherMessages] {
  override def make(ctx: ActorContext[ComponentMsg],
                    assemblyInfo: AssemblyInfo): AssemblyHandlers[DiagPublisherMessages] =
    new TromboneAssemblyHandlers(ctx, assemblyInfo)
}

class TromboneAssemblyHandlers(ctx: ActorContext[ComponentMsg], info: AssemblyInfo)
    extends AssemblyHandlers[DiagPublisherMessages](ctx, info) {

  private var diagPublsher: ActorRef[DiagPublisherMessages] = _

  private var commandHandler: ActorRef[NotFollowingMsgs] = _

  implicit var ac: AssemblyContext  = _
  implicit val ec: ExecutionContext = ctx.executionContext

  val runningHcd: Option[Running] = None

  def onRun(): Unit = ()

  def initialize(): Future[Unit] = async {
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(info, calculationConfig, controlConfig)

    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    commandHandler = ctx.spawnAnonymous(TromboneCommandHandler.make(ac, runningHcd, Some(eventPublisher)))

    diagPublsher = ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcd, Some(eventPublisher)))
  }

  override def onShutdown(): Unit = println("Received Shutdown")

  override def onRestart(): Unit = println("Received dorestart")

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received GoOnline")

  def onDomainMsg(mode: DiagPublisherMessages): Unit = mode match {
    case DiagnosticState => diagPublsher ! DiagnosticState
    case OperationsState => diagPublsher ! OperationsState
    case _               ⇒
  }

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = ???

  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation = {
    val validation = validateOneSetup(s)
    if (validation == Valid) {
      commandHandler ! TromboneCommandHandlerMsgs.Submit(
        s,
        commandOriginator.getOrElse(ctx.spawnAnonymous(Behavior.empty))
      )
    }
    validation
  }

  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation = Validation.Valid
}
