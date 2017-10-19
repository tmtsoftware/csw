package csw.services.commons.componentlogger;

import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerMutableActor;

//#component-logger-mutable-actor
public class JComponentMutableActorLogger extends JComponentLoggerMutableActor<Object> {

    private ILogger log;
    public JComponentMutableActorLogger(ActorContext<Object> ctx, String _componentName) {
        log = getLogger(ctx, _componentName);
    }

    @Override
    public Actor.Receive<Object> createReceive() {
        return null;
    }
}
//#component-logger-mutable-actor

