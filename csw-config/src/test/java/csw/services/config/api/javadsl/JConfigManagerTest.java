package csw.services.config.api.javadsl;

import akka.stream.Materializer;
import csw.services.config.api.commons.TestFileUtils;
import csw.services.config.api.models.ConfigData;
import csw.services.config.api.models.ConfigFileHistory;
import csw.services.config.api.models.ConfigFileInfo;
import csw.services.config.api.models.ConfigId;
import csw.services.config.server.ServerWiring;
import csw.services.config.server.internal.JConfigManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class JConfigManagerTest {
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
        configManager.create(file, ConfigData.fromString(configValue), false, "commit test file").get();
        Optional<ConfigData> configData = configManager.get(file).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue);
    }

    @Test
    public void testCreateOversizeFile() throws ExecutionException, InterruptedException {
        File tempFile = Paths.get("SomeOversizeFile.txt").toFile();
        String configValue = "test {We think, this is some oversize text!!!!}";
        configManager.create(tempFile, ConfigData.fromString(configValue), true).get();
        Optional<ConfigData> configData = configManager.get(tempFile).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue);
    }

    @Test
    public void testUpdateExistingFile() throws ExecutionException, InterruptedException {
        String assemblyConfigValue = "axisName = tromboneAxis";
        File file = Paths.get("/assembly.conf").toFile();
        configManager.create(file, ConfigData.fromString(assemblyConfigValue), false, "commit assembly conf").get();
        Optional<ConfigData> configData = configManager.get(file).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), assemblyConfigValue);

        String updatedAssemblyConfigValue = "assemblyHCDCount = 3";
        configManager.update(file, ConfigData.fromString(updatedAssemblyConfigValue), "commit updated assembly conf").get();
        Optional<ConfigData> configDataUpdated = configManager.get(file).get();
        Assert.assertEquals(configDataUpdated.get().toJStringF(mat).get(), updatedAssemblyConfigValue);
    }

    @Test
    public void testSpecificVersionRetrieval() throws ExecutionException, InterruptedException {
        String configValue = "axisName = tromboneAxis";
        String assemblyConfigValue = "assemblyHCDCount = 3";
        String newAssemblyConfigValue = "assemblyHCDCount = 5";
        File file = Paths.get("/a/b/csw.conf").toFile();
        configManager.create(file, ConfigData.fromString(configValue), false, "commit csw conf file").get();
        Assert.assertEquals(configManager.get(file).get().get().toJStringF(mat).get(), configValue);

        ConfigId configId = configManager.update(file, ConfigData.fromString(assemblyConfigValue), "commit updated conf file").get();

        configManager.update(file, ConfigData.fromString(newAssemblyConfigValue), "updated config to assembly").get();
        Assert.assertEquals(configManager.get(file).get().get().toJStringF(mat).get(), newAssemblyConfigValue);

        Assert.assertEquals(configManager.get(file, Optional.of(configId)).get().get().toJStringF(mat).get(), assemblyConfigValue);
    }

    @Test
    public void testCorrectVersionBasedOnDate() throws ExecutionException, InterruptedException {
        String configValue = "axisName = tromboneAxis";
        String assemblyConfigValue = "assemblyHCDCount = 3";
        String newAssemblyConfigValue = "assemblyHCDCount = 5";

        File file = Paths.get("/test.conf").toFile();
        configManager.create(file, ConfigData.fromString(configValue), false, "commit initial configuration").get();
        Assert.assertEquals(configManager.get(file).get().get().toJStringF(mat).get(), configValue);

        configManager.update(file, ConfigData.fromString(assemblyConfigValue), "updated config to assembly").get();
        Date date = new Date();
        configManager.update(file, ConfigData.fromString(newAssemblyConfigValue), "updated config to assembly").get();

        Assert.assertEquals(configManager.get(file).get().get().toJStringF(mat).get(), newAssemblyConfigValue);
        Assert.assertEquals(configManager.get(file, date).get().get().toJStringF(mat).get(), assemblyConfigValue);
    }

    @Test
    public void testHistoryOfAFile() throws ExecutionException, InterruptedException {
        String configValue = "axisName = tromboneAxis";
        String assemblyConfigValue = "assemblyHCDCount = 3";
        String newAssemblyConfigValue = "assemblyHCDCount = 5";

        File file = Paths.get("/test.conf").toFile();
        ConfigId configIdCreate = configManager.create(file, ConfigData.fromString(configValue), false, "commit initial configuration").get();
        Assert.assertEquals(configManager.get(file).get().get().toJStringF(mat).get(), configValue);

        ConfigId configIdUpdate1 = configManager.update(file, ConfigData.fromString(assemblyConfigValue), "updated config to assembly").get();
        ConfigId configIdUpdate2 = configManager.update(file, ConfigData.fromString(newAssemblyConfigValue), "updated config to assembly").get();

        Assert.assertEquals(configManager.history(file).get().size(), 3);
        Assert.assertEquals(configManager.history(file).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1, configIdCreate)));

        Assert.assertEquals(configManager.history(file, 2).get().size(), 2);
        Assert.assertEquals(configManager.history(file, 2).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1)));
    }

    @Test
    public void testListAllFiles() throws ExecutionException, InterruptedException {
        File tromboneConfig = Paths.get("trombone.conf").toFile();
        File assemblyConfig = Paths.get("a/b/assembly/assembly.conf").toFile();

        String tromboneConfigComment = "hello trombone";
        String assemblyConfigComment = "hello assembly";

        ConfigId tromboneConfigId = configManager.create(tromboneConfig, ConfigData.fromString("axisName = tromboneAxis"), false, tromboneConfigComment).get();
        ConfigId assemblyConfigId = configManager.create(assemblyConfig, ConfigData.fromString("assemblyHCDCount = 3"), false, assemblyConfigComment).get();

        ConfigFileInfo tromboneConfigInfo = new ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment);
        ConfigFileInfo assemblyConfigInfo = new ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment);

        Assert.assertEquals(configManager.list().get(), new ArrayList<>(Arrays.asList(assemblyConfigInfo, tromboneConfigInfo)));
    }
}
