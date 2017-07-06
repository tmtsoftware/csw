package csw.services.logging.components;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.utils.JLogUtil;

public class JTromboneHCD implements JTromboneHCDLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
