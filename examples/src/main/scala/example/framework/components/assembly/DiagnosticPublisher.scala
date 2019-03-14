package example.framework.components.assembly

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.api.scaladsl.CommandService
trait DiagnosticPublisherMessages

object DiagnosticsPublisher {
  def behavior(runningIn: CommandService, worker: ActorRef[WorkerActorMsg]): Behavior[DiagnosticPublisherMessages] =
    Behaviors.setup { ctx ⇒
      // setup required for actor

      Behaviors.receiveMessage {
        case _ ⇒ // Handle messages and return new behavior
          Behaviors.same
      }
    }
}
