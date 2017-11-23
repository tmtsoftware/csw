package csw.services.commons.componentlogger;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

//#component-logger
public class JSample {

    private ILogger log;

    public JSample(String _componentName) {
        this.log = new JLoggerFactory(_componentName).getLogger(getClass());
    }
}
//#component-logger