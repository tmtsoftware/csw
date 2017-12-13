package csw.trombone.assembly.actors

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.ccs.commands.ComponentRef
import csw.messages.location.Connection
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext, TrombonePublisherMsg}

abstract class AssemblyCommandBehaviorFactory {

  protected def assemblyCommandHandlers(
      ctx: ActorContext[AssemblyCommandHandlerMsgs],
      ac: AssemblyContext,
      tromboneHCDs: Map[Connection, Option[ComponentRef]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): AssemblyFollowingCommandHandlers

  def make(
      assemblyContext: AssemblyContext,
      hcds: Map[Connection, Option[ComponentRef]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): Behavior[AssemblyCommandHandlerMsgs] =
    Actor
      .mutable[AssemblyCommandHandlerMsgs](
        ctx ⇒
          new AssemblyCommandBehavior(
            ctx,
            assemblyContext,
            assemblyCommandHandlers(ctx, assemblyContext, hcds, allEventPublisher)
        )
      )
}
