package csw.services.config.server

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.stream.IOResult
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound, InvalidFilePath}
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.files.Sha1
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Future

abstract class ConfigServiceTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val serverWiring = new ServerWiring

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  def configService: ConfigService

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def afterAll(): Unit =
    actorSystem.terminate().await

  val configValue1: String =
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

  val configValue4: String =
    """
      |axisName1111 = tromboneAxis1111
      |axisName2222 = tromboneAxis2222
      |axisName3333 = tromboneAxis3333
      |""".stripMargin

  val configValue5: String =
    """
      |axisName11111 = tromboneAxis11111
      |axisName22222 = tromboneAxis22222
      |axisName33333 = tromboneAxis33333
      |""".stripMargin

  def createConfigs(configFileNames: Set[String]): Set[ConfigId] =
    configFileNames.map(fileName ⇒ {
      val fileContent            = scala.io.Source.fromResource(fileName).mkString
      val configData: ConfigData = ConfigData.fromString(fileContent)
      configService.create(Paths.get(fileName), configData, oversize = false, s"committing file: ${fileName}").await
    })

  implicit class RichInputStream(is: InputStream) {
    def toByteArray() = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  // DEOPSCSW-42: Storing text based component configuration (uploading files with various sizes)
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  test("should able to upload and get component configurations from config service") {
    val configFileNames            = Set("tromboneAssembly.conf", "tromboneContainer.conf", "tromboneHCD.conf")
    val configIds                  = createConfigs(configFileNames)
    val configFilePaths: Set[Path] = configFileNames.map(name ⇒ Paths.get(name))
    val tuples                     = configIds zip configFilePaths

    for {
      (configId, path) ← tuples
    } yield {
      val configData = configService.get(path, Some(configId)).await
      val source     = scala.io.Source.fromResource(path.toString)
      try source.mkString shouldEqual configData.get.toStringF.await
      finally {
        source.close()
      }
    }
  }

  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  test("should able to upload and get binary configurations from config service") {
    val binaryFileName = "binaryConf.bin"
    val binaryConfPath = Paths.get("/tmt/trombone/test/conf/large/" + binaryFileName)

    val binarySourceData: InputStream = getClass().getClassLoader().getResourceAsStream(binaryFileName)
    val expectedBinaryContent         = binarySourceData.toByteArray()

    val stream: Source[ByteString, Future[IOResult]] =
      StreamConverters.fromInputStream(() ⇒ new ByteArrayInputStream(expectedBinaryContent))

    val configData = ConfigData.fromSource(stream)

    configService.create(binaryConfPath, configData, oversize = true, "commit test file").await

    configService.get(binaryConfPath).await.get.toInputStream.toByteArray() shouldBe expectedBinaryContent
  }

  //  DEOPSCSW-42: Storing text based component configuration (exercise deep path)
  test("should able to create a file and retrieve the same") {
    val file = Paths.get("/tmt/trombone/assembly/conf/normalfiles/test/test.conf")
    configService.create(file, ConfigData.fromString(configValue1), oversize = false, "commit test file").await
    configService.get(file).await.get.toStringF.await shouldBe configValue1
  }

  //  DEOPSCSW-42: Storing text based component configuration
  test("should ignore '/' at the beginning of file path and create a file") {
    val fileName             = "csw.conf/1/2/3"
    val file                 = Paths.get(s"/$fileName")
    val fileWithoutBackslash = Paths.get(fileName)
    configService.create(file, ConfigData.fromString(configValue1), oversize = false, "commit csw file").await

    intercept[FileAlreadyExists] {
      configService
        .create(fileWithoutBackslash, ConfigData.fromString(configValue1), oversize = false, "commit without '/'")
        .await
    }

    configService.get(fileWithoutBackslash).await.get.toStringF.await shouldBe configValue1
  }

  //  DEOPSCSW-42: Storing text based component configuration
  test("should throw FileAlreadyExists while creating a file if it already exists in repository") {
    val file = Paths.get("/tmt/tcp/redis/text/redis.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), oversize = false, "commit redis conf for first time")
      .await

    intercept[FileAlreadyExists] {
      configService
        .create(file, ConfigData.fromString(configValue1), oversize = false, "commit redis conf again")
        .await
    }

    val newFile = Paths.get("/tmt/tcp/redis/text/redis_updated.conf")
    val configId = configService
      .create(newFile, ConfigData.fromString(configValue3), oversize = false, "commit redis conf with unique name")
      .await
    configId shouldBe ConfigId(2)
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  test("should able to update existing file and get the file with updated content") {
    val file = Paths.get("/tmt/text/trombone/test/assembly.conf")

    configService.create(file, ConfigData.fromString(configValue1), oversize = false, "commit assembly conf").await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "commit updated assembly conf").await
    configService.get(file).await.get.toStringF.await shouldBe configValue2
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  test("update should throw FileNotFoundException if a file does not exists in repository") {
    val file = Paths.get("/assembly.conf")

    intercept[FileNotFound] {
      configService.update(file, ConfigData.fromString(configValue1), "commit updated assembly conf").await
    }
  }

  //  DEOPSCSW-46: Unique identifier for configuration file version
  test("each revision of file should have unique identifier") {
    val tromboneHcdConf       = Paths.get("trombone/test/hcd/akka/hcd.conf")
    val tromboneAssemblyConf  = Paths.get("trombone/test/assembly/akka/assembly.conf")
    val tromboneContainerConf = Paths.get("trombone/test/container/akka/container.conf")
    val binaryConfPath        = Paths.get("trombone/test/binary/binaryConf.bin")
    val expectedConfigIds     = List(ConfigId(1), ConfigId(2), ConfigId(3), ConfigId(4), ConfigId(5), ConfigId(6))

    val configId1 = configService.create(tromboneHcdConf, ConfigData.fromString(configValue1)).await
    val configId2 = configService.create(tromboneAssemblyConf, ConfigData.fromString(configValue2)).await
    val configId3 = configService.create(binaryConfPath, ConfigData.fromString(configValue3), oversize = true).await
    val configId4 = configService.create(tromboneContainerConf, ConfigData.fromString(configValue4)).await
    val configId5 = configService.update(tromboneHcdConf, ConfigData.fromString(configValue5)).await
    val configId6 = configService.update(tromboneAssemblyConf, ConfigData.fromString(configValue2)).await

    val actualConfigIds =
      List(configId1, configId2, configId3, configId4, configId5, configId6).sortBy(configId ⇒ configId.id)

    actualConfigIds shouldBe expectedConfigIds
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  test("get call should return `None` if a file does not exists in repository") {
    val file = Paths.get("/test.conf")

    configService.get(file).await shouldBe None
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  test("should able to retrieve the specific version of file by config ID") {
    val tromboneHcdConf       = Paths.get("trombone/test/hcd/akka/hcd.conf")
    val tromboneAssemblyConf  = Paths.get("trombone/test/assembly/akka/assembly.conf")
    val tromboneContainerConf = Paths.get("trombone/test/container/akka/container.conf")
    val redisConf             = Paths.get("redis/test/text/redis.conf")

    val configId1 = configService.create(tromboneHcdConf, ConfigData.fromString(configValue1)).await
    val configId2 = configService.create(tromboneAssemblyConf, ConfigData.fromString(configValue2)).await
    val configId3 = configService.create(redisConf, ConfigData.fromString(configValue3)).await
    val configId4 = configService.create(tromboneContainerConf, ConfigData.fromString(configValue4)).await
    val configId5 = configService.update(tromboneHcdConf, ConfigData.fromString(configValue5)).await
    val configId6 = configService.update(tromboneAssemblyConf, ConfigData.fromString(configValue2)).await

    val configData1 = configService.get(tromboneHcdConf, Some(configId1)).await.get
    configData1.toStringF.await shouldBe configValue1

    val configData2 = configService.get(tromboneAssemblyConf, Some(configId2)).await.get
    configData2.toStringF.await shouldBe configValue2

    val configData3 = configService.get(redisConf, Some(configId3)).await.get
    configData3.toStringF.await shouldBe configValue3

    val configData4 = configService.get(tromboneContainerConf, Some(configId4)).await.get
    configData4.toStringF.await shouldBe configValue4

    val configData5 = configService.get(tromboneHcdConf, Some(configId5)).await.get
    configData5.toStringF.await shouldBe configValue5

    val configData6 = configService.get(tromboneAssemblyConf, Some(configId6)).await.get
    configData6.toStringF.await shouldBe configValue2
  }

  test("should get the correct version of file based on date") {
    val file = Paths.get("/test.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), oversize = false, "commit initial configuration")
      .await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val time = Instant.now()
    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.get(file).await.get.toStringF.await shouldBe configValue3
    configService.get(file, time).await.get.toStringF.await shouldBe configValue2
  }

  test("should get the initial version of file if date provided is before the creation date") {
    val time = Instant.MIN
    val file = Paths.get("/test.conf")

    configService
      .create(file, ConfigData.fromString(configValue1), oversize = false, "commit initial configuration")
      .await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await

    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await
    configService.get(file).await.get.toStringF.await shouldBe configValue3

    configService.get(file, time).await.get.toStringF.await shouldBe configValue1
  }

  test("should get the history of a file") {
    val file = Paths.get("/test.conf")
    val configIdCreate = configService
      .create(file, ConfigData.fromString(configValue1), oversize = false, "commit initial configuration")
      .await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    val configIdUpdate1 =
      configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val configIdUpdate2 =
      configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

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

    //  Check that files to be added does not already exists in the repo and then add
    configService.list.await.foreach { fileInfo ⇒
      fileInfo.path should not be tromboneConfig
      fileInfo.path should not be assemblyConfig
    }

    //  Add files to repo
    val tromboneConfigId = configService
      .create(tromboneConfig, ConfigData.fromString(configValue1), oversize = false, tromboneConfigComment)
      .await
    val assemblyConfigId = configService
      .create(assemblyConfig, ConfigData.fromString(configValue2), oversize = false, assemblyConfigComment)
      .await

    val tromboneConfigInfo: ConfigFileInfo = ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment)
    val assemblyConfigInfo: ConfigFileInfo = ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)

    // list files from repo and assert that it contains added files
    configService.list().await shouldBe List(assemblyConfigInfo, tromboneConfigInfo)
  }

  // DEOPSCSW-74: Check config file existence by unique name
  test("exists should return false if file does not exist") {
    val file = Paths.get("/test.conf")

    configService.exists(file).await shouldBe false
  }

  // DEOPSCSW-74: Check config file existence by unique name
  test("exists should return true if file exist") {
    val textFile = Paths.get("a/test.csw.conf")

    configService.create(textFile, ConfigData.fromString(configValue1), oversize = false, "commit config file").await
    configService.exists(textFile).await shouldBe true

    val binaryFile = Paths.get("/tmt/binary/hcd/ref.bin")

    configService.create(binaryFile, ConfigData.fromString(configValue1), oversize = true, "commit config file").await
    configService.exists(binaryFile).await shouldBe true

  }

  test("should able to delete existing file") {
    val file = Paths.get("tromboneHCD.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), oversize = false, "commit trombone config file")
      .await

    configService.get(file).await.get.toStringF.await shouldBe configValue1

    configService.delete(file).await
    configService.get(file).await shouldBe None
  }

  test("deleting non existing file should throw FileNotFoundException") {
    val file = Paths.get("tromboneHCD.conf")
    intercept[FileNotFound] {
      configService.delete(file).await
    }
  }

  test("delete removes all versions of a file") {
    val file = Paths.get("/a/b/csw.conf")

    configService.create(file, ConfigData.fromString(configValue1), oversize = false, "commit config file").await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    val configId = configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.history(file).await.size shouldBe 3
    configService.delete(file).await
    intercept[FileNotFound] {
      configService.history(file).await.size shouldBe 0
    }
    configService.get(file, Some(configId)).await.get.toStringF.await shouldBe configValue2
    configService.get(file, Some(ConfigId(3))).await.get.toStringF.await shouldBe configValue3
    configService.get(file).await shouldBe None
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  test("should able to get, set and reset the default version of config file") {
    // create file
    val file = Paths.get("/tmt/test/setdefault/getdefault/resetdefault/default.conf")
    configService.create(file, ConfigData.fromString(configValue1), oversize = false, "hello world").await
    configService.get(file).await.get.toStringF.await shouldBe configValue1

    // update file twice
    val configId = configService.update(file, ConfigData.fromString(configValue2), "Updated config to assembly").await
    configService.update(file, ConfigData.fromString(configValue3), "Updated config to assembly").await

    // check that get file without ID should return latest file
    configService.get(file).await.get.toStringF.await shouldBe configValue3
    // check that getDefault file without ID should return latest file
    configService.getDefault(file).await.get.toStringF.await shouldBe configValue3
    // set default version of file to id=2
    configService.setDefault(file, Some(configId)).await
    // check that getDefault file without ID returns file with id=2
    configService.getDefault(file).await.get.toStringF.await shouldBe configValue2
    // check that setDefault without id,resets default version of file
    configService.setDefault(file).await
    configService.getDefault(file).await.get.toStringF.await shouldBe configValue3
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  test("getDefault should return None if file does not exists") {
    val file = Paths.get("/tmt/test/ahgvfyfgpp.conf")
    configService.getDefault(file).await shouldBe None
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  test("should be able to store and retrieve oversize file") {
    val file    = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"

    val configData  = ConfigData.fromString(content)
    val configId    = configService.create(file, configData, oversize = true, "committing oversize file").await
    val fileContent = configService.get(file, Some(configId)).await.get
    fileContent.toStringF.await shouldBe content

    val svnConfigData =
      configService.get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(configId)).await.get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  test("should list oversize files without .sha1 suffix") {
    val file1    = Paths.get("OversizeFile1.txt")
    val comment1 = "committing oversize file"

    val file2    = Paths.get("OversizeFile2.txt")
    val comment2 = "committing one more oversize file"

    val configId1 =
      configService.create(file1, ConfigData.fromString("testing oversize file"), oversize = true, comment1).await
    val configId2 =
      configService.create(file2, ConfigData.fromString("testing oversize file"), oversize = true, comment2).await

    val listOfFileInfo: List[ConfigFileInfo] = configService.list().await

    listOfFileInfo.toSet shouldBe Set(
      ConfigFileInfo(file1, configId1, comment1),
      ConfigFileInfo(file2, configId2, comment2)
    )
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  test("should be able to update oversize file and retrieve the history") {
    val file            = Paths.get("SomeOversizeFile.txt")
    val creationContent = "testing oversize file"
    val creationComment = "initial commit"

    val configData       = ConfigData.fromString(creationContent)
    val creationConfigId = configService.create(file, configData, oversize = true, creationComment).await

    val newContent  = "testing oversize file, again"
    val newComment  = "Updating file"
    val configData2 = ConfigData.fromString(newContent)
    val newConfigId = configService.update(file, configData2, newComment).await

    val creationFileContent = configService.get(file, Some(creationConfigId)).await.get
    creationFileContent.toStringF.await shouldBe creationContent

    val updatedFileContent = configService.get(file, Some(newConfigId)).await.get
    updatedFileContent.toStringF.await shouldBe newContent

    val oldSvnConfigData = configService
      .get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(creationConfigId))
      .await
      .get
    oldSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await

    val newSvnConfigData = configService
      .get(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), Some(newConfigId))
      .await
      .get
    newSvnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData2).await

    val fileHistories: List[ConfigFileHistory] = configService.history(file).await

    fileHistories.map(history => (history.id, history.comment)) shouldBe List(
      (newConfigId, newComment),
      (creationConfigId, creationComment)
    )
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  test("should be able to get oversize default file") {
    val file    = Paths.get("SomeOversizeFile.txt")
    val content = "testing oversize file"
    val configId =
      configService.create(file, ConfigData.fromString(content), oversize = true, "committing oversize file").await

    configService.setDefault(file, Some(configId)).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    configService.update(file, ConfigData.fromString(newContent), newComment).await

    val defaultData: ConfigData = configService.getDefault(file).await.get
    defaultData.toStringF.await shouldBe content

    configService.setDefault(file).await

    val resetDefaultData: ConfigData = configService.getDefault(file).await.get
    resetDefaultData.toStringF.await shouldBe newContent

    configService.delete(file, "deleting file").await

    val fileExists = configService.exists(file).await
    fileExists shouldBe false

    val defaultAfterDelete = configService.getDefault(file).await
    defaultAfterDelete shouldBe None

    intercept[FileNotFound] {
      configService.setDefault(file).await
    }
  }

  test("should be able to get oversize time stamped file") {
    val initialTime = Instant.MIN

    val file    = Paths.get("SomeOversizeFile.txt")
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

    val fileTimeStampedAfterDelete = configService.get(file).await
    fileTimeStampedAfterDelete shouldBe None
  }

  test("should allow to create files with valid path and throw error for invalid path") {
    val filePath = Paths.get("/invalid path!/sample@.txt")

    intercept[InvalidFilePath] {
      configService
        .create(filePath, ConfigData.fromString("testing invalid file path"), oversize = false,
          "testing invalid file path")
        .await
    }
  }

  test("should exclude .default files from list") {
    val tromboneConfig = Paths.get("trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    // Add files to repo
    val tromboneConfigId = configService
      .create(tromboneConfig, ConfigData.fromString(configValue1), oversize = false, tromboneConfigComment)
      .await
    val assemblyConfigId = configService
      .create(assemblyConfig, ConfigData.fromString(configValue2), oversize = true, assemblyConfigComment)
      .await

    configService.setDefault(tromboneConfig, Some(tromboneConfigId)).await
    configService.setDefault(assemblyConfig, Some(assemblyConfigId)).await

    // list files from repo and assert that it contains added files
    val configFiles = configService.list().await

    configFiles.toSet shouldBe Set(
      ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment),
      ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)
    )
  }
}
