package csw.services.commons.commonlogger;

import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.commons.ComponentDomainMessage;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

//#common-component-logger-mutable-actor
//JMutableActorSample is used for mutable actor java class only
public abstract class JMutableActorSample extends Actor.MutableBehavior<ComponentDomainMessage> {
    public static ILogger log;

    public JMutableActorSample(ActorContext<ComponentDomainMessage> ctx) {
        log = new JLoggerFactory("my-component-name").getLogger(ctx, getClass());
    }
}
//#common-component-logger-mutable-actor