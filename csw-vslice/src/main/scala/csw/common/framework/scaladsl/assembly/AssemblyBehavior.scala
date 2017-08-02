package csw.common.framework.scaladsl.assembly

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.models.AssemblyMsg.{Oneway, Submit}
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehavior
import csw.param.Parameters
import csw.param.Parameters.{Observe, Setup}

import scala.reflect.ClassTag

class AssemblyBehavior[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg],
                                                   supervisor: ActorRef[FromComponentLifecycleMessage],
                                                   assemblyHandlers: AssemblyHandlers[Msg])
    extends ComponentBehavior[Msg, AssemblyMsg](ctx, supervisor, assemblyHandlers) {

  override def onRunningCompCommandMsg(x: AssemblyMsg): Unit = x match {
    case Submit(command, replyTo) ⇒ onSubmit(command, replyTo)
    case Oneway(command, replyTo) ⇒ onOneWay(command, replyTo)
  }

  private def onSubmit(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case si: Setup   => setupSubmit(si, oneway = false, replyTo)
    case oi: Observe => observeSubmit(oi, oneway = false, replyTo)
  }

  private def onOneWay(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case sca: Setup   => setupSubmit(sca, oneway = true, replyTo)
    case oca: Observe => observeSubmit(oca, oneway = true, replyTo)
  }

  private def setupSubmit(s: Setup, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo       = if (oneway) None else Some(replyTo)
    val validation              = assemblyHandlers.setup(s, completionReplyTo)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  private def observeSubmit(o: Observe, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo = if (oneway) None else Some(replyTo)
    val validation        = assemblyHandlers.observe(o, completionReplyTo)

    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }
}
