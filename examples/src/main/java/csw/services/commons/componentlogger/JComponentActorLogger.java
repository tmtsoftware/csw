package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerActor;

public class JComponentActorLogger extends JComponentLoggerActor {
    private final ILogger log;
    private String componentName;

    public JComponentActorLogger(String _componentName) {
        this.componentName = _componentName;
        this.log = getLogger();
    }

    @Override
    public String componentName() {
        return this.componentName;
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
