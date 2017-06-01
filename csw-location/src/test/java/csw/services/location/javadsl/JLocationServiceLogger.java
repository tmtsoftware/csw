package csw.services.location.javadsl;

import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JComponentLogger;

interface JLocationServiceLogger extends JComponentLogger {
    @Override
    default String componentName() {
        return Constants.LocationService();
    }
}
