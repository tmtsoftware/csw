package csw.services.logging.components.iris;

import csw.services.logging.javadsl.JComponentLogger;

public interface JIrisSimpleLogger extends JComponentLogger {
    String NAME = "jIRIS";

    @Override
    default String componentName() {
        return NAME;
    }
}