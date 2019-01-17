package csw.logging.client.components.trombone;

import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.logging.client.utils.JLogUtil;

public class JTromboneHCDTLA {

    private ILogger logger;

    public JTromboneHCDTLA(JLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(getClass());
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
