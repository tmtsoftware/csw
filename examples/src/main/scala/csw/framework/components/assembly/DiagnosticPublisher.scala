package csw.framework.components.assembly

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior}
import csw.services.command.scaladsl.CommandService

trait DiagnosticPublisherMessages

object DiagnosticsPublisher {

  def make(runningIn: CommandService, worker: ActorRef[WorkerActorMsg]): Behavior[DiagnosticPublisherMessages] =
    Behaviors.setup(ctx ⇒ new DiagnosticsPublisher(ctx, runningIn, worker))
}

class DiagnosticsPublisher(
    ctx: ActorContext[DiagnosticPublisherMessages],
    runningIn: CommandService,
    worker: ActorRef[WorkerActorMsg]
) extends MutableBehavior[DiagnosticPublisherMessages] {
  override def onMessage(msg: DiagnosticPublisherMessages): Behavior[DiagnosticPublisherMessages] = ???
}
