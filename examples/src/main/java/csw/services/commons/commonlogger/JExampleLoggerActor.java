package csw.services.commons.commonlogger;

import csw.services.logging.javadsl.JComponentLoggerActor;

//#jcomponent-logger-actor
//JExampleLoggerActor is used for untyped actor java class only
public abstract class JExampleLoggerActor extends JComponentLoggerActor {
    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#jcomponent-logger-actor