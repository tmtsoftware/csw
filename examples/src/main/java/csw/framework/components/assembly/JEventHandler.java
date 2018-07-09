package csw.framework.components.assembly;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.MutableBehavior;
import csw.messages.events.Event;

public class JEventHandler extends MutableBehavior<Event> {

    private ActorContext<Event> ctx;

    public JEventHandler(ActorContext<Event> context) {
        ctx = context;
    }

    @Override
    public Behaviors.Receive<Event> createReceive() {
        return null;
    }
}
