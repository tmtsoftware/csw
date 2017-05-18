package csw.services.config.client.javadsl.demo;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import csw.services.config.api.javadsl.IConfigClientService;
import csw.services.config.api.javadsl.IConfigService;
import csw.services.config.api.models.ConfigData;
import csw.services.config.api.models.ConfigId;
import csw.services.config.api.models.ConfigMetadata;
import csw.services.config.client.internal.ActorRuntime;
import csw.services.config.client.javadsl.JConfigClientFactory;
import csw.services.config.server.ServerWiring;
import csw.services.config.server.commons.TestFileUtils;
import csw.services.config.server.http.HttpService;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JLocationServiceFactory;
import org.junit.*;
import org.junit.rules.ExpectedException;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class JConfigClientDemoExample {
    private static ActorRuntime actorRuntime = new ActorRuntime(ActorSystem.create());
    private static ILocationService clientLocationService = JLocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552));

    //#create-api
    //config client API
    IConfigClientService clientApi = JConfigClientFactory.clientApi(actorRuntime.actorSystem(), clientLocationService);
    //config admin API
    IConfigService adminApi = JConfigClientFactory.adminApi(actorRuntime.actorSystem(), clientLocationService);
    //#create-api

    private static ServerWiring serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552, new scala.collection.mutable.ArrayBuffer()));
    private static HttpService httpService = serverWiring.httpService();
    private TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());

    private Materializer mat = actorRuntime.mat();

    //#declare_string_config
    String defaultStrConf = "foo { bar { baz : 1234 } }";
    //#declare_string_config

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeAll() throws Exception {
        Await.result(httpService.registeredLazyBinding(), Duration.create(20, "seconds"));
    }

    @Before
    public void initSvnRepo() {
        serverWiring.svnRepo().initSvnRepo();
    }

    @After
    public void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(httpService.shutdown(), Duration.create(20, "seconds"));
        clientLocationService.shutdown().get();
        Await.result(actorRuntime.actorSystem().terminate(), Duration.create(20, "seconds"));
    }


    @Test
    public void testExists() throws ExecutionException, InterruptedException {
        //#exists
        Path filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf");

        // create file using admin API
        adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "commit config file").get();

        Boolean exists = clientApi.exists(filePath).get();
        Assert.assertTrue(exists);
        //#exists
    }

    @Test
    public void testGetActive() throws ExecutionException, InterruptedException {
        //#getActive
        // construct the path
        Path filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf");

        adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "First commit").get();

        ConfigData activeFile = clientApi.getActive(filePath).get().get();
        Assert.assertEquals(activeFile.toJConfigObject(mat).get().getString("foo.bar.baz"), "1234");
        //#getActive
    }

    @Test
    public void testCreateUpdateDelete() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        //#create
        //construct ConfigData from String containing ASCII text
        String configString = "axisName11111 = tromboneAxis\naxisName22222 = tromboneAxis2\naxisName3 = tromboneAxis3333";
        ConfigData config1 = ConfigData.fromString(configString);

        //construct ConfigData from a local file containing binary data
        URI srcFilePath = getClass().getClassLoader().getResource("smallBinary.bin").toURI();
        ConfigData config2 = ConfigData.fromPath(Paths.get(srcFilePath));

        ConfigId id1 = adminApi.create(Paths.get("/hcd/trombone/overnight.conf"), config1, false, "review done").get();
        ConfigId id2 = adminApi.create(Paths.get("/hcd/trombone/firmware.bin"), config2, true, "smoke test done").get();

        //CAUTION: for demo example setup these IDs are returned. Don't assume them in production setup.
        Assert.assertEquals(id1, new ConfigId("1"));
        Assert.assertEquals(id2, new ConfigId("3"));
        //#create
    }

    @Test
    public void testDelete() throws ExecutionException, InterruptedException {
        Path path = Paths.get("tromboneHCD.conf");
        adminApi.create(path, ConfigData.fromString(defaultStrConf), false, "commit trombone config file").get();

        Assert.assertEquals(adminApi.getLatest(path).get().get().toJStringF(mat).get(), defaultStrConf);

        adminApi.delete(path, "no longer needed").get();
        Assert.assertEquals(adminApi.getLatest(path).get(), Optional.empty());
    }

    @Test
    public void testGetMetadata() throws ExecutionException, InterruptedException {
        //#getMetadata
        ConfigMetadata metadata = adminApi.getMetadata().get();
        //repository path must not be empty
        Assert.assertNotEquals(metadata.repoPath(), "");
        System.out.println("Server returned => " + metadata.toString());
        //#getMetadata
    }
}
