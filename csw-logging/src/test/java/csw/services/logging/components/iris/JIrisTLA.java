package csw.services.logging.components.iris;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;
import csw.services.logging.utils.JLogUtil;

public class JIrisTLA {

    private ILogger logger;

    public JIrisTLA(JLoggerFactory loggerFactory) {
        // DEOPSCSW-316: Improve Logger accessibility for component developers
        this.logger = loggerFactory.getLogger(getClass());
    }

    public void startLogging() {
        JLogUtil.logInBulk(logger);
    }
}
