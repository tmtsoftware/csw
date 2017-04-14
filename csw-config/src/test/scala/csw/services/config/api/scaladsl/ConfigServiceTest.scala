package csw.services.config.api.scaladsl

import java.io._
import java.nio.file.Paths
import java.time.Instant

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.commons.TestFileUtils
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.server.ServerWiring
import csw.services.config.server.files.Sha1
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

abstract class ConfigServiceTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val serverWiring = new ServerWiring

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  def configService: ConfigService

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
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit test file").await
    configService.get(file).await.get.toStringF.await shouldBe configValue
  }

  test("should ignore '/' at the beginning of file path and create a file") {
    val fileName = "csw.conf"
    val file = Paths.get(s"/$fileName")
    val fileWithoutBackslash = Paths.get(fileName)
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit csw file").await

    intercept[IOException] {
      configService.create(fileWithoutBackslash, ConfigData.fromString(configValue), oversize = false, "commit without '/'").await
    }

    configService.get(fileWithoutBackslash).await.get.toStringF.await shouldBe configValue
  }

  test("should throw IOException while creating a file if it already exists in repository") {
    val file = Paths.get("/test.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit test conf for first time").await

    intercept[IOException] {
      configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit test conf again").await
    }
  }

  test("should able to update existing file and get the file with updated content") {
    val file = Paths.get("/assembly.conf")

    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit assembly conf").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    configService.update(file, ConfigData.fromString(configValue2), "commit updated assembly conf").await
    configService.get(file).await.get.toStringF.await shouldBe configValue2
  }

  test("update should throw FileNotFoundException if a file does not exists in repository") {
    val file = Paths.get("/assembly.conf")

    intercept[FileNotFoundException] {
      configService.update(file, ConfigData.fromString(configValue), "commit updated assembly conf").await
    }
  }

  test("get call should return `None` if a file does not exists in repository") {
    val file = Paths.get("/test.conf")

    configService.get(file).await shouldBe None
  }

  test("should able to retrieve the specific version of file by config ID") {
    val file = Paths.get("/a/b/csw.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit csw conf file").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    val configId = configService.update(file, ConfigData.fromString(configValue), "commit updated conf file").await

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    configService.get(file).await.get.toStringF.await shouldBe configValue2

    configService.get(file, Some(configId)).await.get.toStringF.await shouldBe configValue
  }

  test("should get the correct version of file based on date") {
    val file = Paths.get("/test.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val time = Instant.now()
    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.get(file).await.get.toStringF.await shouldBe configValue3
    configService.get(file, time).await.get.toStringF.await shouldBe configValue2
  }

  test("should get the initial version of file if date provided is before the creation date") {
    val time = Instant.MIN
    val file = Paths.get("/test.conf")

    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await

    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await
    configService.get(file).await.get.toStringF.await shouldBe configValue3

    configService.get(file, time).await.get.toStringF.await shouldBe configValue
  }

  test("should get the history of a file") {
    val file = Paths.get("/test.conf")
    val configIdCreate = configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit initial configuration").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    val configIdUpdate1 = configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val configIdUpdate2 = configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.history(file).await.size shouldBe 3
    configService.history(file).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate1, configIdCreate)

    configService.history(file, 2).await.size shouldBe 2
    configService.history(file, 2).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate1)
  }

  test("should list all the available config files") {
    val tromboneConfig = Paths.get("trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    val tromboneConfigId = configService.create(tromboneConfig, ConfigData.fromString("axisName = tromboneAxis"), oversize = false, tromboneConfigComment).await
    val assemblyConfigId = configService.create(assemblyConfig, ConfigData.fromString("assemblyHCDCount = 3"), oversize = false, assemblyConfigComment).await

    val tromboneConfigInfo: ConfigFileInfo = ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment)
    val assemblyConfigInfo: ConfigFileInfo = ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)

    configService.list().await shouldBe List(assemblyConfigInfo, tromboneConfigInfo)
  }

  test("exists should return false if file does not exist") {
    val file = Paths.get("/test.conf")

    configService.exists(file).await shouldBe false
  }

  test("exists should return true if file exist") {
    val file = Paths.get("a/test.csw.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit config file").await

    configService.exists(file).await shouldBe true
  }

  test("should able to delete existing file") {
    val file = Paths.get("tromboneHCD.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit trombone config file").await

    configService.get(file).await.get.toStringF.await shouldBe configValue

    configService.delete(file).await
    configService.get(file).await shouldBe None
  }

  test("deleting non existing file should throw FileNotFoundException") {
    val file = Paths.get("tromboneHCD.conf")
    intercept[FileNotFoundException] {
      configService.delete(file).await
    }
  }

  //  TODO Implementation of delete() needs to be fixed
  //  This is not a valid test. Delete should just remove latest version and keep older ones.
  test("delete removes all versions of a file") {
    val file = Paths.get("/a/b/csw.conf")

    configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit config file").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    val configId = configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.history(file).await.size shouldBe 3
    configService.delete(file).await
    configService.history(file).await.size shouldBe 0
    configService.get(file, Some(configId)).await.get.toStringF.await shouldBe configValue2
    configService.get(file, Some(ConfigId(3))).await.get.toStringF.await shouldBe configValue3
    configService.get(file).await shouldBe None
  }

  test("should able to get and set the default config file") {
    val file = Paths.get("/test.conf")
    configService.create(file, ConfigData.fromString(configValue), oversize = false, "hello world").await
    configService.get(file).await.get.toStringF.await shouldBe configValue

    val configIdUpdate1 = configService.update(file, ConfigData.fromString(configValue2), "Updated config to assembly").await
    configService.update(file, ConfigData.fromString(configValue3), "Updated config to assembly").await

    configService.getDefault(file).await.get.toStringF.await shouldBe configValue3
    configService.setDefault(file, Some(configIdUpdate1)).await
    configService.getDefault(file).await.get.toStringF.await shouldBe configValue2
    configService.resetDefault(file).await
    configService.getDefault(file).await.get.toStringF.await shouldBe configValue3
  }

  test("should be able to store and retrieve oversize file") {
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"

    val configData = ConfigData.fromString(content)
    val configId = configService.create(file, configData, oversize = true, "committing oversize file").await
    val fileContent = configService.get(file, Some(configId)).await.get
    fileContent.toStringF.await shouldBe content

    val svnConfigData = configService.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(configId)).await.get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  test("should list oversize files") {
    val file1 = Paths.get("OversizeFile1.txt")
    val comment1  = "committing oversize file"

    val file2 = Paths.get("OversizeFile2.txt")
    val comment2 = "committing one more oversize file"

    val configId1 = configService.create(file1, ConfigData.fromString("testing oversize file"), oversize = true, comment1).await
    val configId2 = configService.create(file2, ConfigData.fromString("testing oversize file"), oversize = true, comment2).await

    val listOfFileInfo: List[ConfigFileInfo] = configService.list().await

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
    val creationConfigId = configService.create(file, configData, oversize = true, creationComment).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    val configData2 = ConfigData.fromString(newContent)
    val newConfigId = configService.update(file, configData2, newComment).await

    val creationFileContent = configService.get(file, Some(creationConfigId)).await.get
    creationFileContent.toStringF.await shouldBe creationContent

    val updatedFileContent = configService.get(file, Some(newConfigId)).await.get
    updatedFileContent.toStringF.await shouldBe newContent

    val oldSvnConfigData = configService.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(creationConfigId)).await.get
    oldSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await

    val newSvnConfigData = configService.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(newConfigId)).await.get
    newSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData2).await

    val fileHistories: List[ConfigFileHistory] = configService.history(file).await

    fileHistories.map(history => (history.id, history.comment)) shouldBe List(
      (newConfigId, newComment),
      (creationConfigId, creationComment)
    )
  }

  test("should be able to get oversize default file") {
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"
    configService.create(file, ConfigData.fromString(content), oversize = true, "committing oversize file").await

    configService.setDefault(file).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    configService.update(file, ConfigData.fromString(newContent), newComment).await

    val defaultData: ConfigData = configService.getDefault(file).await.get
    defaultData.toStringF.await shouldBe content

    configService.resetDefault(file).await

    val resetDefaultData: ConfigData = configService.getDefault(file).await.get
    resetDefaultData.toStringF.await shouldBe newContent

    configService.delete(file, "deleting file").await

    val fileExists = configService.exists(file).await
    fileExists shouldBe false

    val defaultAfterDelete = configService.getDefault(file).await
    defaultAfterDelete shouldBe None

    intercept[java.io.FileNotFoundException] {
      configService.resetDefault(file).await
    }
  }

  test("should be able to get oversize time stamped file") {
    val initialTime = Instant.MIN
    
    val file = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"
    configService.create(file, ConfigData.fromString(content), oversize = true, "committing oversize file").await

    val time = Instant.now()

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    configService.update(file, ConfigData.fromString(newContent), newComment).await

    val initialData = configService.get(file, initialTime).await.get
    initialData.toStringF.await shouldBe content

    val oldTimeStampedData = configService.get(file, time).await.get
    oldTimeStampedData.toStringF.await shouldBe content

    val latestData = configService.get(file, Instant.now()).await.get
    latestData.toStringF.await shouldBe newContent

    configService.delete(file, "deleting file").await

    val fileExists = configService.exists(file).await
    fileExists shouldBe false

    val fileTimeStampedAfterDelete = configService.get(file, Instant.now()).await
    fileTimeStampedAfterDelete shouldBe None
  }
}
