package csw.logging.core.components.trombone;

import csw.logging.core.javadsl.ILogger;
import csw.logging.core.javadsl.JLoggerFactory;
import csw.logging.core.utils.JLogUtil;

public class JTromboneHCDTLA {

    private ILogger logger;

    public JTromboneHCDTLA(JLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(getClass());
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
