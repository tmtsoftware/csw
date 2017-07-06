package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.JComponentLogger;

public interface JTromboneHCDSimpleLogger extends JComponentLogger {
    @Override
    default String componentName() {
        return "tromboneHcd";
    }
}