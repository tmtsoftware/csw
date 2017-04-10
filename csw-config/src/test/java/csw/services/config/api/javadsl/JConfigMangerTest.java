package csw.services.config.api.javadsl;

import akka.stream.Materializer;
import csw.services.config.api.commons.TestFileUtils;
import csw.services.config.api.models.ConfigData;
import csw.services.config.server.ServerWiring;
import csw.services.config.server.internal.JConfigManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class JConfigMangerTest {
    private ServerWiring serverWiring = new ServerWiring();
    private TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());

    private IConfigManager configManager = new JConfigManager(serverWiring.configManager(), serverWiring.actorRuntime());
    private Materializer mat = serverWiring.actorRuntime().mat();

    @Before
    public void initSvnRepo() {
        serverWiring.svnAdmin().initSvnRepo();
    }

    @After
    public void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    @Test
    public void testCreateAndRetrieveFile() throws ExecutionException, InterruptedException {
        String configValue = "axisName = tromboneAxis";

        File file = Paths.get("test.conf").toFile();
        configManager.create(file, ConfigData.apply(configValue), false, "commit test file").get();
        Optional<ConfigData> configData = configManager.get(file).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue);
    }

    @Test
    public void testCreateOversizeFile() throws ExecutionException, InterruptedException {
        File tempFile = Paths.get("SomeOversizeFile.txt").toFile();
        String configValue = "test {We think, this is some oversize text!!!!}";
        configManager.create(tempFile, ConfigData.apply(configValue), true).get();
        Optional<ConfigData> configData = configManager.get(tempFile).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue);
    }

    @Test
    public void testUpdateExistingFile() throws ExecutionException, InterruptedException {
        String assemblyConfigValue = "axisName = tromboneAxis";
        File file = Paths.get("/assembly.conf").toFile();
        configManager.create(file, ConfigData.apply(assemblyConfigValue), false, "commit assembly conf").get();
        Optional<ConfigData> configData = configManager.get(file).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), assemblyConfigValue);

        String updatedAssemblyConfigValue = "assemblyHCDCount = 3";
        configManager.update(file, ConfigData.apply(updatedAssemblyConfigValue), "commit updated assembly conf").get();
        Optional<ConfigData> configDataUpdated = configManager.get(file).get();
        Assert.assertEquals(configDataUpdated.get().toJStringF(mat).get(), updatedAssemblyConfigValue);
    }

}
