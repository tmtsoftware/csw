package csw.common.framework.scaladsl.assembly

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation.Validation
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models._
import csw.param.Parameters.{Observe, Setup}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class AssemblyHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg], info: AssemblyInfo) {
  val runningHcd: Option[HcdResponseMode.Running] = None

  var isOnline: Boolean = false

  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext

  def initialize(): Future[Unit]
  def onRun(): Unit
  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation
  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit
}
