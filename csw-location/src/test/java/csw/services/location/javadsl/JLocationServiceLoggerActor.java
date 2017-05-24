package csw.services.location.javadsl;


import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JComponentLoggerActor;

import java.util.Optional;

public abstract class JLocationServiceLoggerActor extends JComponentLoggerActor {
    @Override
    public Optional<String> componentName() {
        return Optional.of(Constants.LocationService());
    }
}
