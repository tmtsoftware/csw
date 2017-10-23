package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;

//#component-logger
public class JSample implements JComponentLogger {

    private ILogger log;

    public JSample(String _componentName) {
        this.log = getLogger(_componentName);
    }
}
//#component-logger