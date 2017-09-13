package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;
import csw.services.logging.utils.JLogUtil;

public class JTromboneHCDTLA implements JComponentLogger {

    private ILogger logger;
    private String componentName;

    public JTromboneHCDTLA(String componentName) {
        this.componentName = componentName;
        this.logger = getLogger();
    }

    public void startLogging() {
        new JLogUtil().logInBulk(logger);
    }

    @Override
    public String componentName() {
        return this.componentName;
    }
}
