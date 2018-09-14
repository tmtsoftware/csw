package csw.logging.components.trombone;

import csw.logging.javadsl.ILogger;
import csw.logging.javadsl.JLoggerFactory;
import csw.logging.utils.JLogUtil;

public class JTromboneHCDTLA {

    private ILogger logger;

    public JTromboneHCDTLA(JLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(getClass());
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
