package csw.config.client.javadsl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import csw.config.api.ConfigData;
import csw.config.api.javadsl.IConfigClientService;
import csw.config.api.javadsl.IConfigService;
import csw.config.client.JConfigClientBaseSuite;
import csw.config.models.ConfigId;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

// DEOPSCSW-138:Split Config API into Admin API and Client API
// DEOPSCSW-103: Java API for Configuration service
public class JConfigClientApiTest extends JUnitSuite {

    private static JConfigClientBaseSuite jConfigClientBaseSuite;
    private static IConfigService configService;
    private static IConfigClientService configClientApi;
    private static ActorSystem<SpawnProtocol.Command> system;

    @BeforeClass
    public static void beforeAll() throws Exception {
        jConfigClientBaseSuite = new JConfigClientBaseSuite();
        jConfigClientBaseSuite.setup();
        configService = jConfigClientBaseSuite.configService;
        configClientApi = jConfigClientBaseSuite.configClientApi;
        system = jConfigClientBaseSuite.system;
    }

    @Before
    public void initSvnRepo() {
        jConfigClientBaseSuite.initSvnRepo();
    }

    @After
    public void deleteServerFiles() {
        jConfigClientBaseSuite.deleteServerFiles();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        jConfigClientBaseSuite.cleanup();
    }

    private final String configValue1 = "axisName1 = tromboneAxis\naxisName2 = tromboneAxis2\naxisName3 = tromboneAxis3";
    private final String configValue2 = "axisName11 = tromboneAxis\naxisName22 = tromboneAxis2\naxisName3 = tromboneAxis33";
    private final String configValue3 = "axisName111 = tromboneAxis\naxisName222 = tromboneAxis2\naxisName3 = tromboneAxis333";

    // DEOPSCSW-88: Config service API implementation details need to be hidden from component developer
    @Test
    public void testConfigClientApi__DEOPSCSW_138_DEOPSCSW_103_DEOPSCSW_88() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/text-files/trombone_hcd/application.conf");

        configService.create(path, ConfigData.fromString(configValue1), false, "first commit").get();
        Assert.assertTrue(configClientApi.exists(path).get());

        ConfigId configId1 = configService.update(path, ConfigData.fromString(configValue2), "second commit").get();
        configService.update(path, ConfigData.fromString(configValue3), "third commit").get();
        Assert.assertEquals(configClientApi.getActive(path).get().orElseThrow().toJStringF(system).get(), configValue1);
        Assert.assertTrue(configClientApi.exists(path, configId1).get());

        configService.setActiveVersion(path, configId1, "setting active version").get();
        Assert.assertEquals(configClientApi.getActive(path).get().orElseThrow().toJStringF(system).get(), configValue2);

        configService.resetActiveVersion(path, "resetting active version of file").get();
        Assert.assertEquals(configClientApi.getActive(path).get().orElseThrow().toJStringF(system).get(), configValue3);
    }
}
