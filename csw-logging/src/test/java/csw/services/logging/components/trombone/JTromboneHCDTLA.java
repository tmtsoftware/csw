package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;
import csw.services.logging.utils.JLogUtil;

public class JTromboneHCDTLA implements JComponentLogger {

    private ILogger logger;

    public JTromboneHCDTLA(String componentName) {
        this.logger = getLogger(componentName);
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
