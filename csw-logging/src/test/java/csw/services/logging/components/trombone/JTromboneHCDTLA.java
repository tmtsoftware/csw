package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.utils.JLogUtil;

public class JTromboneHCDTLA implements JTromboneHCDSimpleLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }
}
