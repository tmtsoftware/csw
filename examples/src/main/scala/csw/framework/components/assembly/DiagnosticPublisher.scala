package csw.framework.components.assembly

import akka.actor.typed.scaladsl.Behaviors.MutableBehavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.services.command.scaladsl.CommandService

trait DiagnosticPublisherMessages

object DiagnosticsPublisher {
  def make(
      runningIn: Option[CommandService],
      worker: Option[ActorRef[WorkerActorMsg]]
  ): Behavior[DiagnosticPublisherMessages] =
    Behaviors.mutable(ctx ⇒ new DiagnosticsPublisher(ctx, runningIn, worker))
}

class DiagnosticsPublisher(
    ctx: ActorContext[DiagnosticPublisherMessages],
    runningIn: Option[CommandService],
    worker: Option[ActorRef[WorkerActorMsg]]
) extends MutableBehavior[DiagnosticPublisherMessages] {
  override def onMessage(msg: DiagnosticPublisherMessages): Behavior[DiagnosticPublisherMessages] = ???
}
