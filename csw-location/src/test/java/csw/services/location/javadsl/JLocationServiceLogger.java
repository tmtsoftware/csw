package csw.services.location.javadsl;

import csw.services.location.commons.Constants;
import csw.services.logging.javadsl.JCommonComponentLogger;

interface JLocationServiceLogger extends JCommonComponentLogger {
    @Override
    default String componentName() {
        return Constants.LocationService();
    }
}
