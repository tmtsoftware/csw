package csw.services.commons.componentlogger;

import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.commons.ComponentDomainMessage;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerMutableActor;

//#component-logger-mutable-actor
public class JMutableActorSample extends JComponentLoggerMutableActor<ComponentDomainMessage> {

    private ILogger log;
    public JMutableActorSample(ActorContext<ComponentDomainMessage> ctx, String _componentName) {
        log = getLogger(ctx, _componentName);
    }

    @Override
    public Actor.Receive<ComponentDomainMessage> createReceive() {
        return null;
    }
}
//#component-logger-mutable-actor

