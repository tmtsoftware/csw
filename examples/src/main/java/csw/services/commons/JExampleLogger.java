package csw.services.commons;

import csw.services.logging.javadsl.JComponentLogger;

public interface JExampleLogger extends JComponentLogger {
    @Override
    default String componentName() {
        return "examples";
    }
}
