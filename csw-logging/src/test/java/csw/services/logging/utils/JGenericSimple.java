package csw.services.logging.utils;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLogger;

public class JGenericSimple implements JGenericLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

}
