package csw.logging.genericlogger;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.internal.messages.ComponentMessage;
import csw.logging.javadsl.ILogger;
import csw.logging.javadsl.JGenericLoggerFactory;

//#generic-logger-class
public class JGenericClass {

    ILogger log = JGenericLoggerFactory.getLogger(getClass());
}
//#generic-logger-class

//#generic-logger-actor
class JGenericActor extends akka.actor.AbstractActor {

    //context() is available from akka.actor.AbstractActor
    ILogger log = JGenericLoggerFactory.getLogger(context(), getClass());

    @Override
    public Receive createReceive() {
        return null;
    }
}
//#generic-logger-actor

//#generic-logger-typed-actor
class JGenericTypedActor {

    public JGenericTypedActor(ActorContext<ComponentMessage> ctx) {
        ILogger log = JGenericLoggerFactory.getLogger(ctx, getClass());
    }
}
//#generic-logger-typed-actor
