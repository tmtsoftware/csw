package csw.common.framework.scaladsl.assembly

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation.Validation
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.RunningAssemblyMsg.AssemblyDomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.LifecycleHandlers
import csw.param.Parameters.{Observe, Setup}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

abstract class AssemblyHandlers[Msg <: AssemblyDomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg], info: AssemblyInfo)
    extends LifecycleHandlers[Msg] {
  val runningHcd: Option[HcdResponseMode.Running] = None

  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext

  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation
  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation
}
