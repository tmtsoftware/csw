package csw.services.config.api.scaladsl

import java.io._
import java.nio.file.Paths
import java.time.Instant

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.commons.TestFileUtils
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo}
import csw.services.config.server.ServerWiring
import csw.services.config.server.files.Sha1
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

abstract class ConfigServiceTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  lazy val serverWiring = new ServerWiring

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  def configManager: ConfigService

  override protected def beforeEach(): Unit = {
    serverWiring.svnAdmin.initSvnRepo()
  }

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
  }

  val configValue: String =
    """
      |axisName1 = tromboneAxis1
      |axisName2 = tromboneAxis2
      |axisName3 = tromboneAxis3
      |""".stripMargin

  val configValue2: String =
    """
      |axisName11 = tromboneAxis11
      |axisName22 = tromboneAxis22
      |axisName33 = tromboneAxis33
      |""".stripMargin

  val configValue3: String =
    """
      |axisName111 = tromboneAxis111
      |axisName222 = tromboneAxis222
      |axisName333 = tromboneAxis333
      |""".stripMargin


  test("should able to create a file and retrieve the same") {
    val file = Paths.get("test.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit test file").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue
  }

  test("should ignore '/' at the beginning of file path and create a file") {
    val fileName = "csw.conf"
    val file = Paths.get(s"/$fileName")
    val fileWithoutBackslash = Paths.get(fileName)
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit csw file").await

    intercept[IOException] {
      configManager.create(fileWithoutBackslash, ConfigData.fromString(configValue), oversize = false, "commit without '/'").await
    }

    configManager.get(fileWithoutBackslash).await.get.toStringF.await shouldBe configValue
  }

  test("should throw IOException while creating a file if it already exists in repository") {
    val file = Paths.get("/test.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit test conf for first time").await

    intercept[IOException] {
      configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit test conf again").await
    }
  }

  test("should able to update existing file and get the file with updated content") {
    val file = Paths.get("/assembly.conf")

    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit assembly conf").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    configManager.update(file, ConfigData.fromString(configValue2), "commit updated assembly conf").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue2
  }

  test("update should throw FileNotFoundException if a file does not exists in repository") {
    val file = Paths.get("/assembly.conf")

    intercept[FileNotFoundException] {
      configManager.update(file, ConfigData.fromString(configValue), "commit updated assembly conf").await
    }
  }

  test("get call should return `None` if a file does not exists in repository") {
    val file = Paths.get("/test.conf")

    configManager.get(file).await shouldBe None
  }

  test("should able to retrieve the specific version of file by config ID") {
    val file = Paths.get("/a/b/csw.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit csw conf file").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    val configId = configManager.update(file, ConfigData.fromString(configValue), "commit updated conf file").await

    configManager.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue2

    configManager.get(file, Some(configId)).await.get.toStringF.await shouldBe configValue
  }

  test("should get the correct version of file based on date") {
    val file = Paths.get("/test.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    configManager.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val time = Instant.now()
    configManager.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configManager.get(file).await.get.toStringF.await shouldBe configValue3
    configManager.get(file, time).await.get.toStringF.await shouldBe configValue2
  }

  test("should get the initial version of file if date provided is before the creation date") {
    val time = Instant.MIN
    val file = Paths.get("/test.conf")

    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    configManager.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await

    configManager.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue3

    configManager.get(file, time).await.get.toStringF.await shouldBe configValue
  }

  test("should get the history of a file") {
    val file = Paths.get("/test.conf")
    val configIdCreate = configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    val configIdUpdate1 = configManager.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val configIdUpdate2 = configManager.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.history(file).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate1, configIdCreate)

    configManager.history(file, 2).await.size shouldBe 2
    configManager.history(file, 2).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate1)
  }

  test("should list all the available config files") {
    val tromboneConfig = Paths.get("trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    val tromboneConfigId = configManager.create(tromboneConfig, ConfigData.fromString("axisName = tromboneAxis"), oversize = false, tromboneConfigComment).await
    val assemblyConfigId = configManager.create(assemblyConfig, ConfigData.fromString("assemblyHCDCount = 3"), oversize = false, assemblyConfigComment).await

    val tromboneConfigInfo: ConfigFileInfo = ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment)
    val assemblyConfigInfo: ConfigFileInfo = ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)

    configManager.list().await shouldBe List(assemblyConfigInfo, tromboneConfigInfo)
  }

  test("exists should return false if file does not exist") {
    val file = Paths.get("/test.conf")

    configManager.exists(file).await shouldBe false
  }

  test("exists should return true if file exist") {
    val file = Paths.get("a/test.csw.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit config file").await

    configManager.exists(file).await shouldBe true
  }

  test("should able to delete existing file") {
    val file = Paths.get("tromboneHCD.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit trombone config file").await

    configManager.get(file).await.get.toStringF.await shouldBe configValue

    configManager.delete(file).await
    configManager.get(file).await shouldBe None
  }

  test("deleting non existing file should throw FileNotFoundException") {
    val file = Paths.get("tromboneHCD.conf")
    intercept[FileNotFoundException] {
      configManager.delete(file).await
    }
  }

  //  TODO Implementation of delete() needs to be fixed
  //  This is not a valid test. Delete should just remove latest version and keep older ones.
  test("delete removes all versions of a file") {
    val file = Paths.get("/a/b/csw.conf")

    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "commit config file").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    val configId = configManager.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    configManager.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.delete(file).await
    configManager.history(file).await.size shouldBe 0
    configManager.get(file, Some(configId)).await shouldBe None
  }

  test("should able to get and set the default config file") {
    val file = Paths.get("/test.conf")
    configManager.create(file, ConfigData.fromString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.toStringF.await shouldBe configValue

    val configIdUpdate1 = configManager.update(file, ConfigData.fromString(configValue2), "Updated config to assembly").await
    configManager.update(file, ConfigData.fromString(configValue3), "Updated config to assembly").await

    configManager.getDefault(file).await.get.toStringF.await shouldBe configValue3
    configManager.setDefault(file, Some(configIdUpdate1)).await
    configManager.getDefault(file).await.get.toStringF.await shouldBe configValue2
    configManager.resetDefault(file).await
    configManager.getDefault(file).await.get.toStringF.await shouldBe configValue3
  }

  test("should be able to store and retrieve oversize file") {
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"

    val configData = ConfigData.fromString(content)
    val configId = configManager.create(file, configData, oversize = true, "committing oversize file").await
    val fileContent = configManager.get(file, Some(configId)).await.get
    fileContent.toStringF.await shouldBe content

    val svnConfigData = configManager.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(configId)).await.get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  test("should list oversize files") {
    val file1 = Paths.get("OversizeFile1.txt")
    val comment1  = "committing oversize file"

    val file2 = Paths.get("OversizeFile2.txt")
    val comment2 = "committing one more oversize file"

    val configId1 = configManager.create(file1, ConfigData.fromString("testing oversize file"), oversize = true, comment1).await
    val configId2 = configManager.create(file2, ConfigData.fromString("testing oversize file"), oversize = true, comment2).await

    val listOfFileInfo: List[ConfigFileInfo] = configManager.list().await

    listOfFileInfo.toSet shouldBe Set(
      ConfigFileInfo(Paths.get(s"${file1.toString}${serverWiring.settings.`sha1-suffix`}"), configId1, comment1),
      ConfigFileInfo(Paths.get(s"${file2.toString}${serverWiring.settings.`sha1-suffix`}"), configId2, comment2)
    )
  }

  test("should be able to update oversize file and retrieve the history") {
    val file = Paths.get("SomeOversizeFile.txt")
    val creationContent = "testing oversize file"
    val creationComment = "initial commit"

    val configData = ConfigData.fromString(creationContent)
    val creationConfigId = configManager.create(file, configData, oversize = true, creationComment).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    val configData2 = ConfigData.fromString(newContent)
    val newConfigId = configManager.update(file, configData2, newComment).await

    val creationFileContent = configManager.get(file, Some(creationConfigId)).await.get
    creationFileContent.toStringF.await shouldBe creationContent

    val updatedFileContent = configManager.get(file, Some(newConfigId)).await.get
    updatedFileContent.toStringF.await shouldBe newContent

    val oldSvnConfigData = configManager.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(creationConfigId)).await.get
    oldSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await

    val newSvnConfigData = configManager.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(newConfigId)).await.get
    newSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData2).await

    val fileHistories: List[ConfigFileHistory] = configManager.history(file).await

    fileHistories.map(history => (history.id, history.comment)) shouldBe List(
      (newConfigId, newComment),
      (creationConfigId, creationComment)
    )
  }

  test("should be able to get oversize default file") {
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"
    configManager.create(file, ConfigData.fromString(content), oversize = true, "committing oversize file").await

    configManager.setDefault(file).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    configManager.update(file, ConfigData.fromString(newContent), newComment).await

    val defaultData: ConfigData = configManager.getDefault(file).await.get
    defaultData.toStringF.await shouldBe content

    configManager.resetDefault(file).await

    val resetDefaultData: ConfigData = configManager.getDefault(file).await.get
    resetDefaultData.toStringF.await shouldBe newContent

    configManager.delete(file, "deleting file").await

    val fileExists = configManager.exists(file).await
    fileExists shouldBe false

    val defaultAfterDelete = configManager.getDefault(file).await
    defaultAfterDelete shouldBe None

    intercept[java.io.FileNotFoundException] {
      configManager.resetDefault(file).await
    }
  }

  test("should be able to get oversize time stamped file") {
    val initialTime = Instant.MIN
    
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"
    configManager.create(file, ConfigData.fromString(content), oversize = true, "committing oversize file").await

    val time = Instant.now()

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    configManager.update(file, ConfigData.fromString(newContent), newComment).await

    val initialData = configManager.get(file, initialTime).await.get
    initialData.toStringF.await shouldBe content

    val oldTimeStampedData = configManager.get(file, time).await.get
    oldTimeStampedData.toStringF.await shouldBe content

    val latestData = configManager.get(file, Instant.now()).await.get
    latestData.toStringF.await shouldBe newContent

    configManager.delete(file, "deleting file").await

    val fileExists = configManager.exists(file).await
    fileExists shouldBe false

    val fileTimeStampedAfterDelete = configManager.get(file, Instant.now()).await
    fileTimeStampedAfterDelete shouldBe None
  }
}
