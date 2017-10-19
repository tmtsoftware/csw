package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;

public class JComponentSimpleLogger implements JComponentLogger {

    private ILogger log;
    private String componentName;

    public JComponentSimpleLogger(String _componentName) {
        this.componentName = _componentName;
        this.log = getLogger();
    }

    @Override
    public String componentName() {
        return componentName;
    }
}
