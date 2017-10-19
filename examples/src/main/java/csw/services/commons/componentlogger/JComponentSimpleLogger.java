package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;

public class JComponentSimpleLogger implements JComponentLogger {

    private ILogger log;

    public JComponentSimpleLogger(String _componentName) {
        this.log = getLogger(_componentName);
    }
}
