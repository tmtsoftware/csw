package csw.services.commons.commonlogger;

import csw.services.logging.javadsl.JCommonComponentLogger;

//#common-component-logger
//JExampleLogger is used for non-actor java class only
public interface JExampleLogger extends JCommonComponentLogger {
    @Override
    default String componentName() {
        return "my-component-name";
    }
}
//#common-component-logger
