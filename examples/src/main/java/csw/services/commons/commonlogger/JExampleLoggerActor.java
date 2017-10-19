package csw.services.commons.commonlogger;

import csw.services.logging.javadsl.JCommonComponentLoggerActor;

//#jcomponent-logger-actor
//JExampleLoggerActor is used for untyped actor java class only
public abstract class JExampleLoggerActor extends JCommonComponentLoggerActor {
    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#jcomponent-logger-actor