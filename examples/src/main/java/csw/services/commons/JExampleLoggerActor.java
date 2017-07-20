package csw.services.commons;

import csw.services.logging.javadsl.JComponentLoggerActor;

//#jcomponent-logger-actor
//JExampleLoggerActor is used for actor java class only
public abstract class JExampleLoggerActor extends JComponentLoggerActor {
    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#jcomponent-logger-actor