package csw.services.location.javadsl;


import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JCommonComponentLoggerActor;

public abstract class JLocationServiceLoggerActor extends JCommonComponentLoggerActor {
    @Override
    public String componentName() {
        return Constants.LocationService();
    }
}
