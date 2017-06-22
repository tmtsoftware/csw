package csw.services.config;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import csw.services.config.api.javadsl.IConfigClientService;
import csw.services.config.api.javadsl.IConfigService;
import csw.services.config.api.models.ConfigData;
import csw.services.config.api.models.ConfigFileRevision;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class JConfigClientExampleTest {
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

        //#update
        Path destPath = Paths.get("/hcd/trombone/overnight.conf");
        ConfigId newId = adminApi.update(destPath, ConfigData.fromString(defaultStrConf), "added debug statements").get();

        //validate the returned id
        Assert.assertEquals(newId, new ConfigId("5"));
        //#update

        //#delete
        Path unwantedFilePath = Paths.get("/hcd/trombone/overnight.conf");
        adminApi.delete(unwantedFilePath, "no longer needed").get();
        Assert.assertEquals(adminApi.getLatest(unwantedFilePath).get(), Optional.empty());
        //#delete
    }

    @Test
    public void testGetById() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        //#getById
        Path filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf");
        ConfigId id = adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "First commit").get();

        //validate
        ConfigData actualData = adminApi.getById(filePath, id).get().get();
        Assert.assertEquals(defaultStrConf, actualData.toJStringF(mat).get());
        //#getById
    }

    @Test
    public void testGetLatest() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        //#getLatest
        //create a file
        Path filePath = Paths.get("/test.conf");
        ConfigId id = adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "initial configuration").get();

        //override the contents
        String newContent = "I changed the contents!!!";
        adminApi.update(filePath, ConfigData.fromString(newContent), "changed!!").get();

        //get the latest file
        ConfigData newConfigData = adminApi.getLatest(filePath).get().get();
        //validate
        Assert.assertEquals(newConfigData.toJStringF(mat).get(), newContent);
        //#getLatest
    }

    @Test
    public void testGetByTime() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        //#getByTime
        Instant tInitial = Instant.now();

        //create a file
        Path filePath = Paths.get("/test.conf");
        ConfigId id = adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "initial configuration").get();

        //override the contents
        String newContent = "I changed the contents!!!";
        adminApi.update(filePath, ConfigData.fromString(newContent), "changed!!").get();

        ConfigData initialData = adminApi.getByTime(filePath, tInitial).get().get();
        Assert.assertEquals(defaultStrConf, initialData.toJStringF(mat).get());

        ConfigData latestData = adminApi.getByTime(filePath, Instant.now()).get().get();
        Assert.assertEquals(newContent, latestData.toJStringF(mat).get());
        //#getByTime
    }

    @Test
    public void testHistory() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        //#history
        Path filePath = Paths.get("/a/test.conf");
        ConfigId id0 = adminApi.create(filePath, ConfigData.fromString(defaultStrConf), false, "first commit").get();

        //override the contents twice
        Instant tBeginUpdate = Instant.now();
        ConfigId id1 = adminApi.update(filePath, ConfigData.fromString("changing contents"), "second commit").get();
        ConfigId id2 = adminApi.update(filePath, ConfigData.fromString("changing contents again"), "third commit").get();
        Instant tEndUpdate = Instant.now();

        //full file history
        List<ConfigFileRevision> fullHistory = adminApi.history(filePath).get();
        Assert.assertEquals(fullHistory.stream().map(ConfigFileRevision::id).collect(Collectors.toList()), new ArrayList<>(Arrays.asList(id2, id1, id0)));
        Assert.assertEquals(fullHistory.stream().map(ConfigFileRevision::comment).collect(Collectors.toList()), new ArrayList<>(Arrays.asList("third commit", "second commit", "first commit")));

        //drop initial revision and take only update revisions

        //#history
    }

        @Test
    public void testGetMetadata() throws ExecutionException, InterruptedException {
        //#getMetadata
        ConfigMetadata metadata = adminApi.getMetadata().get();
        //repository path must not be empty
        Assert.assertNotEquals(metadata.repoPath(), "");
        //#getMetadata
    }
}
