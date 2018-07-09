package csw.framework.components.assembly

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import csw.messages.events.Event

object EventHandler {
  def make(): Behavior[Event] = Behaviors.setup(ctx ⇒ new EventHandler(ctx))
}

class EventHandler(ctx: ActorContext[Event]) extends MutableBehavior[Event] {
  override def onMessage(msg: Event): Behavior[Event] = ???
}
