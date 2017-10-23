package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerActor;

//#component-logger-actor
public class JActorSample extends JComponentLoggerActor {

    private ILogger log;

    public JActorSample(String _componentName) {
        this.log = getLogger(_componentName);
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
//#component-logger-actor
