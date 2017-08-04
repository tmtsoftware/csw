package csw.common.framework.scaladsl.assembly

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.ccs.CommandStatus
import csw.common.framework.models.AssemblyMsg.{Oneway, Submit}
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehavior

import scala.reflect.ClassTag

class AssemblyBehavior[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg],
                                                   supervisor: ActorRef[FromComponentLifecycleMessage],
                                                   assemblyHandlers: AssemblyHandlers[Msg])
    extends ComponentBehavior[Msg, AssemblyMsg](ctx, supervisor, assemblyHandlers) {

  def onRunningCompCommandMsg(msg: AssemblyMsg): Unit = {
    val newMsg: AssemblyMsg = msg match {
      case x: Oneway ⇒ x.copy(replyTo = ctx.spawnAnonymous(Actor.ignore))
      case x: Submit ⇒ x
    }
    val validation              = assemblyHandlers.onControlCommand(newMsg)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    msg.replyTo ! validationCommandResult
  }
}
