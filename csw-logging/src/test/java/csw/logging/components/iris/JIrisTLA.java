package csw.logging.components.iris;

import csw.logging.javadsl.ILogger;
import csw.logging.javadsl.JLoggerFactory;
import csw.logging.utils.JLogUtil;

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
