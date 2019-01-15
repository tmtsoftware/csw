package csw.logging.core.utils;

import csw.logging.core.javadsl.ILogger;
import csw.logging.core.javadsl.JGenericLoggerFactory;

public class JGenericSimple {
    private ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
