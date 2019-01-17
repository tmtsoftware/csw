package csw.logging.client.utils;

import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JGenericLoggerFactory;

public class JGenericSimple {
    private ILogger logger = JGenericLoggerFactory.getLogger(getClass());

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
