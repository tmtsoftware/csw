package csw.logging.utils;

import csw.logging.javadsl.ILogger;
import csw.logging.javadsl.JGenericLoggerFactory;

public class JGenericSimple {
    private ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
