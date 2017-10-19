package csw.services.logging.components.iris;

import csw.services.logging.javadsl.JCommonComponentLogger;

public interface JIrisSimpleLogger extends JCommonComponentLogger {
    String NAME = "jIRIS";

    @Override
    default String componentName() {
        return NAME;
    }
}