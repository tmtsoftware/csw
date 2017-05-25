package csw.services.location.javadsl;


import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JComponentLoggerActor;

public abstract class JLocationServiceLoggerActor extends JComponentLoggerActor {
    @Override
    public String componentName() {
        return Constants.LocationService();
    }
}
