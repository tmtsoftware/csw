package csw.services.commons.componentlogger;

import akka.actor.AbstractActor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

//#component-logger-actor
public class JActorSample extends AbstractActor {

    private ILogger log;

    public JActorSample(String _componentName) {
        this.log = new JLoggerFactory(_componentName).getLogger(context(), getClass());
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
//#component-logger-actor
