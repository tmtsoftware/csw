package csw.services.location.javadsl;

import csw.services.location.common.ActorRuntime;
import csw.services.location.common.Networks;
import csw.services.location.models.*;
import org.junit.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class JLocationServiceImplTest {
    static ILocationService locationService;
    static ActorRuntime actorRuntime;

    @BeforeClass
    public static void setUp(){
        actorRuntime = JActorRuntime.create("test-java");
        locationService = new JLocationServiceImpl(actorRuntime);
    }

    @After
    public void unregister(){
        locationService.unregisterAll();
    }

    @AfterClass
    public static void shutdown(){
        actorRuntime.actorSystem().terminate();
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

    @Test
    public void testListComponents(){
        int Port = 1234;
        ComponentId componentId =  JComponentId.componentId("configService", JComponentType.Service);
        Connection.HttpConnection connection = JConnection.httpConnection(componentId);
        String Path = "path123";
        URI uri = null;

        try {
            try {
                locationService.register(JRegistration.httpRegistration(connection, Port, Path)).get();
                uri = new URI("http://" + Networks.getPrimaryIpv4Address().getHostAddress() + ":" + Port + "/" + Path);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            ArrayList<Location> locations = new ArrayList<>();
            locations.add(JLocation.resolvedHttpLocation(connection, uri, Path));

            Assert.assertEquals(locations,locationService.list().get());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
