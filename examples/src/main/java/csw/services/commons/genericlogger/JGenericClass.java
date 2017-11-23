package csw.services.commons.genericlogger;

import akka.actor.AbstractActor;
import akka.typed.javadsl.Actor;
import csw.services.commons.ComponentDomainMessage;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLoggerFactory;

//#generic-logger-class
public class JGenericClass {

    ILogger log = JGenericLoggerFactory.getLogger(getClass());
}
//#generic-logger-class

//#generic-logger-actor
class JGenericActor extends akka.actor.AbstractActor {

    ILogger log = JGenericLoggerFactory.getLogger(context(), getClass());

    @Override
    public Receive createReceive() {
        return null;
    }
}
//#generic-logger-actor

//#generic-logger-typed-actor
class JGenericTypedActor {

    public JGenericTypedActor(akka.typed.javadsl.ActorContext<ComponentDomainMessage> ctx) {
        ILogger log = JGenericLoggerFactory.getLogger(ctx, getClass());
    }
}
//#generic-logger-typed-actor
