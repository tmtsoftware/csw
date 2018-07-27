package csw.services.event;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.messages.events.Event;

public class JEventHandlerFactory {
    public static Behavior<Event> make() {
        return Behaviors.setup(JEventHandler::new);
    }
}
