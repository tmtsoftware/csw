package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import csw.command.client.MiniCRM.CRMMessage
import csw.command.client.MiniCRM.MiniCRMMessage.{AddStarted, AddResponse}
import csw.framework.scaladsl.TopLevelComponent.{Oneway2, RunningMessage, Submit2, Validate2}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id

object CommandHelper {

  def apply(
      id: Id,
      tla: ActorRef[RunningMessage],
      cmdMsg: ControlCommand,
      crm: ActorRef[CRMMessage],
      replyTo: ActorRef[SubmitResponse]
  ): Behavior[CommandResponse] = {
    println(s"Started CommandHelper for: $replyTo and ID: $id")
    Behaviors.setup { context =>
      tla ! Validate2(id, cmdMsg, context.self.narrow[ValidateCommandResponse])

      Behaviors.receiveMessage[CommandResponse] {
        case a: Accepted =>
          println(s"Command with id: $id Accepted")
          //replyTo ! a.asInstanceOf[SubmitResponse]
          tla ! Submit2(id, cmdMsg, context.self)
          Behaviors.same
        case i: Invalid =>
          println(s"Command is invalid: $id ${i.issue}")
          replyTo ! i
          Behaviors.stopped
        case c: Completed =>
          println(s"$c: $id")
          crm ! AddResponse(c)
          replyTo ! c
          Behaviors.stopped
        case e: Error =>
          println(s"$e: $id")
          crm ! AddResponse(e)
          replyTo ! e
          Behaviors.stopped
        case c: Cancelled =>
          println(s"$c: $id")
          crm ! AddResponse(c)
          replyTo ! c
          Behaviors.stopped
        case s: Started =>
          println(s"Started: $id")
          crm ! AddStarted(s)
          replyTo ! s
          Behaviors.same
      } /*.receiveSignal {
        case (context: ActorContext[CommandResponse], PostStop) =>
          println(s"PostStop signal for command $id received")
          Behaviors.same
      }
       */
    }
  }
}

object ValidateHelper {

  def apply(id: Id, replyTo: ActorRef[ValidateResponse]): Behavior[ValidateCommandResponse] = {
    println(s"Started ValidateHelper for: $replyTo, $Id")

    Behaviors
      .receiveMessage[ValidateCommandResponse] {
        case vr: ValidateCommandResponse =>
          println(s"Helping with $vr")
          replyTo ! vr.asInstanceOf[ValidateResponse]
          Behaviors.stopped
        case other =>
          println(s"ERROR: Helper received unexpected: $other")
          Behaviors.same
      }
      .receiveSignal {
        case (context: ActorContext[ValidateCommandResponse], PostStop) =>
          println(s"PostStop signal for $id receive")
          Behaviors.same
      }
  }

}

object OnewayHelper {

  def apply(
      id: Id,
      tla: ActorRef[RunningMessage],
      cmdMsg: ControlCommand,
      replyTo: ActorRef[OnewayResponse]
  ): Behavior[ValidateCommandResponse] = {
    println(s"Started Oneway for: $replyTo, $Id")

    Behaviors.setup { context =>
      tla ! Validate2(id, cmdMsg, context.self.narrow[ValidateCommandResponse])

      Behaviors
        .receiveMessage[ValidateCommandResponse] {
          case r @ Accepted(id) =>
            println(s"Oneway with id: $id Accepted")
            replyTo ! r.asInstanceOf[OnewayResponse]
            tla ! Oneway2(id, cmdMsg)
            Behaviors.stopped
          case r @ Invalid(id, _) =>
            println(s"Oneway with id: $id Invalid")
            replyTo ! r.asInstanceOf[OnewayResponse]
            Behaviors.stopped
          case other =>
            println(s"ERROR: Helper received unexpected: $other")
            Behaviors.same
        }
        .receiveSignal {
          case (context: ActorContext[ValidateCommandResponse], PostStop) =>
            println(s"PostStop signal for $id receive")
            Behaviors.same
        }
    }

  }
}
