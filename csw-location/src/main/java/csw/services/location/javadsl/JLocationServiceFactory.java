package csw.services.location.javadsl;

import csw.services.location.common.ActorRuntime;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.LocationServiceFactory;

public class JLocationServiceFactory {
    public static ILocationService make(ActorRuntime actorRuntime) {
        LocationService locationService = LocationServiceFactory.make(actorRuntime);
        return new JLocationServiceImpl(locationService, actorRuntime);
    }
}
