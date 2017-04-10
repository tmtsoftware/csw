package csw.services.config.api.javadsl;

import csw.services.config.api.commons.TestFileUtils;
import csw.services.config.server.ServerWiring;
import csw.services.config.server.internal.JConfigManager;
import csw.services.config.api.models.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


public class JConfigMangerTest {
    private static ServerWiring serverWiring = new ServerWiring();
    private static TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());

    private IConfigManager configManager = new JConfigManager(serverWiring.configManager(), serverWiring.actorRuntime());

    @Before
    public void initSvnRepo() {
        serverWiring.svnAdmin().initSvnRepo();
    }

    @AfterClass
    public static void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    @Test
    public void testCreateAndRetrieveFile() throws ExecutionException, InterruptedException {
        String configValue = "axisName = tromboneAxis";

        File file = Paths.get("test.conf").toFile();
        configManager.create(file, new ConfigString(configValue), false, "commit test file").toCompletableFuture().get();
        Optional<ConfigData> configData = configManager.get(file).toCompletableFuture().get();
        Assert.assertEquals(configData.get().toString(), configValue);
    }

}
