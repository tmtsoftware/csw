package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;
import csw.services.logging.utils.JLogUtil;

public class JTromboneHCDTLA {

    private ILogger logger;

    public JTromboneHCDTLA(JLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(getClass());
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
