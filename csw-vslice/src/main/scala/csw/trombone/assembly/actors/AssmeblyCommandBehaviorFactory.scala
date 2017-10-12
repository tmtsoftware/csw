package csw.trombone.assembly.actors

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.SupervisorExternalMessage
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext, TrombonePublisherMsg}

abstract class AssmeblyCommandBehaviorFactory {

  protected def assemblyCommandHandlers(
      ctx: ActorContext[AssemblyCommandHandlerMsgs],
      ac: AssemblyContext,
      tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): AssemblyCommandHandlers

  def make(
      assemblyContext: AssemblyContext,
      hcd: Option[ActorRef[SupervisorExternalMessage]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): Behavior[AssemblyCommandHandlerMsgs] =
    Actor
      .mutable[AssemblyCommandHandlerMsgs](
        ctx ⇒
          new AssemblyCommandBehavior(
            ctx,
            assemblyContext,
            hcd,
            assemblyCommandHandlers(ctx, assemblyContext, hcd, allEventPublisher)
        )
      )
}
