package csw.services.logging.components.iris;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.utils.JLogUtil;

public class JIrisTLA implements JIrisSimpleLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
