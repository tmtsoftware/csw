package csw.services.commons.commonlogger;

import csw.services.logging.javadsl.JComponentLogger;

//#jcomponent-logger
//JExampleLogger is used for non-actor java class only
public interface JExampleLogger extends JComponentLogger {
    @Override
    default String componentName() {
        return "my-component-name";
    }
}
//#jcomponent-logger
