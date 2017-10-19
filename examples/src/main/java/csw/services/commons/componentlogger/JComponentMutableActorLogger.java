package csw.services.commons.componentlogger;

import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.logging.internal.LogControlMessages;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerMutableActor;

public class JComponentMutableActorLogger extends JComponentLoggerMutableActor<LogControlMessages> {

    private ILogger log;
    public JComponentMutableActorLogger(ActorContext<LogControlMessages> ctx, String _componentName) {
        log = getLogger(ctx, _componentName);
    }

    @Override
    public Actor.Receive<LogControlMessages> createReceive() {
        return null;
    }
}
