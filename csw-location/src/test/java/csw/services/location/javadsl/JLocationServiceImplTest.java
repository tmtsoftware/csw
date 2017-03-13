package csw.services.location.javadsl;

import csw.services.location.common.ActorRuntime;
import csw.services.location.models.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;

public class JLocationServiceImplTest {
    ILocationService locationService;
    ActorRuntime actorRuntime;

    @Before
    public void setUp(){
        actorRuntime = JActorRuntime.create("test-java");
        locationService = new JLocationServiceImpl(actorRuntime);
    }

    @Test
    public void testRegistrationOfHttpComponent(){


        int Port = 1234;
        ComponentId componentId =  JComponentId.componentId("configService", JComponentType.Service);
        Connection.HttpConnection connection = JConnection.httpConnection(componentId);
        String Path = "path123";

        try {
            RegistrationResult registrationResult =  locationService.register(JRegistration.httpRegistration(connection, Port, Path)).get();
            Assert.assertEquals(componentId, registrationResult.componentId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
