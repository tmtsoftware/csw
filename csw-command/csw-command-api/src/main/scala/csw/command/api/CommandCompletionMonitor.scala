package csw.command.api

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import csw.command.api.CommandCompletionMonitor.MonitorStart.AddCommand
import csw.params.commands.CommandResponse.SubmitResponse

import scala.concurrent.Future

object CommandCompletionMonitor {

  type Handler = () => Future[SubmitResponse]

  sealed trait MonitorStart

  object MonitorStart {
    case class AddCommand(query: Handler) extends MonitorStart
  }

  sealed trait MonitorCompletion
  object MonitorCompletion {
    case class Completed(finalResponse: SubmitResponse) extends MonitorCompletion
  }

  def make(listener: ActorRef[MonitorCompletion]): Behavior[MonitorStart] =
    Behaviors.setup(_ => process(listener, Seq.empty[Handler]))

    def process(listener: ActorRef[MonitorCompletion], subcommands: Seq[Handler]): Behavior[MonitorStart] = {

      Behaviors.receive[MonitorStart] { (_, message) =>
        message match {
          case AddCommand(ac) =>
            process(listener, subcommands :+ ac)
        }
      }

      Behaviors.same
    }
}
