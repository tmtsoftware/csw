package csw.services.logging.components;

import csw.services.logging.javadsl.JComponentLogger;

public interface JTromboneHCDLogger extends JComponentLogger {
    @Override
    default String componentName() {
        return "tromboneHcd";
    }
}