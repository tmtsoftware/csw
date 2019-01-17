package csw.logging.client.components.iris;

import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.logging.client.utils.JLogUtil;

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
