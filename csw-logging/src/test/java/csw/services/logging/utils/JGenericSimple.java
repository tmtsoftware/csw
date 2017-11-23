package csw.services.logging.utils;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLogger;
import csw.services.logging.javadsl.JGenericLoggerFactory;

public class JGenericSimple {
    private ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
