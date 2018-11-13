package csw.framework.components.assembly

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.api.scaladsl.CommandService
trait DiagnosticPublisherMessages

object DiagnosticsPublisher {

  def make(runningIn: CommandService, worker: ActorRef[WorkerActorMsg]): Behavior[DiagnosticPublisherMessages] =
    Behaviors.setup(ctx ⇒ new DiagnosticsPublisher(ctx, runningIn, worker))
}

class DiagnosticsPublisher(
    ctx: ActorContext[DiagnosticPublisherMessages],
    runningIn: CommandService,
    worker: ActorRef[WorkerActorMsg]
) extends AbstractBehavior[DiagnosticPublisherMessages] {
  override def onMessage(msg: DiagnosticPublisherMessages): Behavior[DiagnosticPublisherMessages] = ???
}
