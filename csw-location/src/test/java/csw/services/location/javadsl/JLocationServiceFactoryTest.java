package csw.services.location.javadsl;

import csw.services.location.scaladsl.ActorRuntime;
import org.junit.Assert;
import org.junit.Test;

public class JLocationServiceFactoryTest {
    /*@Test
    public void testAbleToCreateLocationServiceUsingParameterlessMake() {
        ILocationService locationService = JLocationServiceFactory.make();
        Assert.assertNotNull(locationService);
    }*/

    @Test
    public void testAbleToCreateLocationServiceProvidingActorRuntime() {
        //#Location-service-creation-using-actor-runtime
        ActorRuntime actorRuntime = new ActorRuntime();
        ILocationService locationService = JLocationServiceFactory.make(actorRuntime);
        //#Location-service-creation-using-actor-runtime
        Assert.assertNotNull(locationService);
        actorRuntime.terminate();
    }
}
