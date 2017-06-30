package csw.services.logging.components;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.utils.LogUtil;

public class JTromboneHCD implements JTromboneHCDLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new LogUtil().logInBulk(logger);
    }
}
