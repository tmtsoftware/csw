package csw.services.location.javadsl;

import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JComponentLogger;

import java.util.Optional;

interface JLocationServiceLogger extends JComponentLogger {
    @Override
    default Optional<String> componentName() {
        return Optional.of(Constants.LocationService());
    }
}
