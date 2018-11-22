package csw.config.server

import java.io.InputStream
import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.stream.scaladsl.StreamConverters
import com.typesafe.config.{Config, ConfigFactory}
import csw.commons.tagobjects.FileSystemSensitive
import csw.config.api.exceptions.{FileAlreadyExists, FileNotFound}
import csw.config.api.models._
import csw.config.api.scaladsl.ConfigService
import csw.config.server.commons.TestFileUtils
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.files.Sha1
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

abstract class ConfigServiceTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  def serverWiring: ServerWiring
  def configService: ConfigService

  private lazy val wiring: ServerWiring = serverWiring
  private lazy val testFileUtils        = new TestFileUtils(wiring.settings)

  import wiring.actorRuntime._

  override protected def beforeEach(): Unit = serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit = testFileUtils.deleteServerFiles()

  override protected def afterAll(): Unit = actorSystem.terminate().await

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
      configService.create(Paths.get(fileName), configData, annex = false, s"committing file: $fileName").await
    })

  implicit class RichInputStream(is: InputStream) {
    def toByteArray: Array[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  // DEOPSCSW-42: Storing text based component configuration (uploading files with various sizes)
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  // DEOPSCSW-48: Store new configuration file in Config. service
  test("should able to upload and get component configurations from config service") {
    val configFileNames            = Set("tromboneAssembly.conf", "tromboneContainer.conf", "tromboneHCD.conf")
    val configIds                  = createConfigs(configFileNames)
    val configFilePaths: Set[Path] = configFileNames.map(name ⇒ Paths.get(name))
    val tuples                     = configIds zip configFilePaths

    for {
      (configId, path) ← tuples
    } yield {
      val configData = configService.getById(path, configId).await
      val source     = scala.io.Source.fromResource(path.toString)
      try source.mkString shouldEqual configData.get.toStringF.await
      finally {
        source.close()
      }
    }
  }

  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  // DEOPSCSW-48: Store new configuration file in Config. service
  // DEOPSCSW-27: Storing binary component configurations
  // DEOPSCSW-81: Storing large files in the configuration service
  // DEOPSCSW-131: Detect and handle oversize files
  test("should able to upload and get binary configurations from config service") {
    val binaryFileName = "binaryConf.bin"
    val binaryConfPath = Paths.get("tmt/trombone/test/conf/large/" + binaryFileName)

    def binarySourceData = getClass.getClassLoader.getResourceAsStream(binaryFileName)
    val binaryContent    = binarySourceData.toByteArray

    val configData = ConfigData.from(StreamConverters.fromInputStream(() ⇒ binarySourceData), binaryContent.length)

    configService.create(binaryConfPath, configData, annex = true, "commit test file").await

    val actualBytes = configService.getLatest(binaryConfPath).await.get.toInputStream.toByteArray

    actualBytes shouldBe binaryContent

    configService.list(Some(FileType.Annex)).await.map(_.path) shouldEqual List(binaryConfPath)
  }

  //  DEOPSCSW-42: Storing text based component configuration (exercise deep path)
  test("should able to create a file and retrieve the same") {
    val file = Paths.get("/tmt/trombone/assembly/conf/normalfiles/test/test.conf")
    configService.create(file, ConfigData.fromString(configValue1), annex = false, "commit test file").await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1
  }

  //  DEOPSCSW-42: Storing text based component configuration
  test("should ignore '/' at the beginning of file path and create a file") {
    val fileName             = "csw.conf/1/2/3"
    val file                 = Paths.get(s"/$fileName")
    val fileWithoutBackslash = Paths.get(fileName)
    configService.create(file, ConfigData.fromString(configValue1), annex = false, "commit csw file").await

    intercept[FileAlreadyExists] {
      configService
        .create(fileWithoutBackslash, ConfigData.fromString(configValue1), annex = false, "commit without '/'")
        .await
    }

    configService.getLatest(fileWithoutBackslash).await.get.toStringF.await shouldBe configValue1
  }

  // DEOPSCSW-42: Storing text based component configuration
  // DEOPSCSW-48: Store new configuration file in Config. service
  // DEOPSCSW-47: Unique name for configuration file
  test("should throw FileAlreadyExists while creating a file if it already exists in repository - assume svn first") {
    val file = Paths.get("/tmt/tcp/redis/text/redis.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), annex = false, "commit redis conf for first time")
      .await

    intercept[FileAlreadyExists] {
      configService
        .create(file, ConfigData.fromString(configValue1), annex = false, "commit redis conf again")
        .await
    }

    intercept[FileAlreadyExists] {
      configService
        .create(file, ConfigData.fromString(configValue1), annex = true, "commit redis conf again")
        .await
    }

    // If this succeeds without exception, then the creation of a new file succeeded
    val newFile = Paths.get("/tmt/tcp/redis/text/redis_updated.conf")
    configService
      .create(newFile, ConfigData.fromString(configValue3), annex = false, "commit redis conf with unique name")
      .await
  }

  // DEOPSCSW-42: Storing text based component configuration
  // DEOPSCSW-48: Store new configuration file in Config. service
  // DEOPSCSW-47: Unique name for configuration file
  test("should throw FileAlreadyExists while creating a file if it already exists in repository - assume annex first") {
    val file = Paths.get("/tmt/tcp/redis/text/redis.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), annex = true, "commit redis conf for first time")
      .await

    intercept[FileAlreadyExists] {
      configService
        .create(file, ConfigData.fromString(configValue1), annex = true, "commit redis conf again")
        .await
    }

    intercept[FileAlreadyExists] {
      configService
        .create(file, ConfigData.fromString(configValue1), annex = false, "commit redis conf again")
        .await
    }

    // If this succeeds without exception, then the creation of a new file succeeded
    val newFile = Paths.get("/tmt/tcp/redis/text/redis_updated.conf")
    configService
      .create(newFile, ConfigData.fromString(configValue3), annex = true, "commit redis conf with unique name")
      .await
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  test("should able to update existing file and get the file with updated content") {
    val file = Paths.get("/tmt/text/trombone/test/assembly.conf")

    configService.create(file, ConfigData.fromString(configValue1), annex = false, "commit assembly conf").await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "commit updated assembly conf").await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue2
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  test("update should throw FileNotFoundException if a file does not exists in repository") {
    val file = Paths.get("/assembly.conf")

    intercept[FileNotFound] {
      configService.update(file, ConfigData.fromString(configValue1), "commit updated assembly conf").await
    }
  }

  // DEOPSCSW-46: Unique identifier for configuration file version
  test("each revision of file should have unique identifier") {
    val tromboneHcdConf       = Paths.get("trombone/test/hcd/akka/hcd.conf")
    val tromboneAssemblyConf  = Paths.get("trombone/test/assembly/akka/assembly.conf")
    val tromboneContainerConf = Paths.get("trombone/test/container/akka/container.conf")
    val binaryConfPath        = Paths.get("trombone/test/binary/binaryConf.bin")
    val expectedConfigIds     = List(ConfigId(1), ConfigId(3), ConfigId(5), ConfigId(7), ConfigId(9), ConfigId(10))

    //consumes 2 revisions, one for actual file one for active file
    val configId1 = configService
      .create(tromboneHcdConf, ConfigData.fromString(configValue1), comment = "creating tromboneHCD.conf")
      .await
    //consumes 2 revisions, one for actual file one for active file
    val configId2 = configService
      .create(tromboneAssemblyConf, ConfigData.fromString(configValue2), comment = "creating tromboneAssembly.conf")
      .await
    //consumes 2 revisions, one for actual file one for active file
    val configId3 = configService
      .create(binaryConfPath, ConfigData.fromString(configValue3), annex = true, "creating binary conf file")
      .await
    //consumes 2 revisions, one for actual file one for active file
    val configId4 = configService
      .create(tromboneContainerConf, ConfigData.fromString(configValue4), comment = "creating trombone container.conf")
      .await

    val configId5 = configService
      .update(tromboneHcdConf, ConfigData.fromString(configValue5), comment = "updating tromboneHCD.conf")
      .await
    val configId6 = configService
      .update(tromboneAssemblyConf, ConfigData.fromString(configValue2), comment = "updating tromboneAssembly.conf")
      .await

    val actualConfigIds = List(configId1, configId2, configId3, configId4, configId5, configId6)

    actualConfigIds shouldBe expectedConfigIds
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  test("get call should return `None` if a file does not exists in repository") {
    val file = Paths.get("/test.conf")

    configService.getLatest(file).await shouldBe None
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  // DEOPSCSW-71: Retrieve any version of a configuration file using its unique id
  // DEOPSCSW-45: Saving version information for config. file
  test("should able to retrieve the specific version of file by config ID") {
    val tromboneHcdConf       = Paths.get("trombone/test/hcd/akka/hcd.conf")
    val tromboneAssemblyConf  = Paths.get("trombone/test/assembly/akka/assembly.conf")
    val tromboneContainerConf = Paths.get("trombone/test/container/akka/container.conf")
    val redisConf             = Paths.get("redis/test/text/redis.conf")

    val configId1 = configService
      .create(tromboneHcdConf, ConfigData.fromString(configValue1), comment = "creating tromboneHCD conf")
      .await
    val configId2 = configService
      .create(tromboneAssemblyConf, ConfigData.fromString(configValue2), comment = "creating tromboneHCD conf")
      .await
    val configId3 =
      configService.create(redisConf, ConfigData.fromString(configValue3), comment = "creating redis conf").await
    val configId4 = configService
      .create(tromboneContainerConf, ConfigData.fromString(configValue4), comment = "creating trombone container conf")
      .await
    val configId5 = configService
      .update(tromboneHcdConf, ConfigData.fromString(configValue5), comment = "updating tromboneHCD conf")
      .await
    val configId6 = configService
      .update(tromboneAssemblyConf, ConfigData.fromString(configValue2), comment = "updating tromboneHCD conf")
      .await

    val configData1 = configService.getById(tromboneHcdConf, configId1).await.get
    configData1.toStringF.await shouldBe configValue1

    val configData2 = configService.getById(tromboneAssemblyConf, configId2).await.get
    configData2.toStringF.await shouldBe configValue2

    val configData3 = configService.getById(redisConf, configId3).await.get
    configData3.toStringF.await shouldBe configValue3

    val configData4 = configService.getById(tromboneContainerConf, configId4).await.get
    configData4.toStringF.await shouldBe configValue4

    val configData5 = configService.getById(tromboneHcdConf, configId5).await.get
    configData5.toStringF.await shouldBe configValue5

    val configData6 = configService.getById(tromboneAssemblyConf, configId6).await.get
    configData6.toStringF.await shouldBe configValue2
  }

  test("should get the correct version of file based on time") {
    val file = Paths.get("/test.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), annex = false, "commit initial configuration")
      .await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val time = Instant.now()
    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    configService.getLatest(file).await.get.toStringF.await shouldBe configValue3
    configService.getByTime(file, time).await.get.toStringF.await shouldBe configValue2
  }

  test("should get the initial version of file if date provided is before the creation date") {
    val time = Instant.MIN
    val file = Paths.get("/test.conf")

    configService
      .create(file, ConfigData.fromString(configValue1), annex = false, "commit initial configuration")
      .await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await

    configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue3

    configService.getByTime(file, time).await.get.toStringF.await shouldBe configValue1
  }

  //DEOPSCSW-85 Record of time and date when config file is created/updated
  test("should record datetime for creation of config file") {
    val file       = Paths.get("/tmt/lgs/trombone/hcd.conf")
    val commitMsg1 = "commit version: 1"

    val configData = ConfigData.fromString(configValue1)
    val now        = Instant.now()
    configService.create(file, configData, annex = false, commitMsg1).await

    val configFileHistories = configService.history(file).await

    val expectedRecordedTimeSpread = now.toEpochMilli +- 100 //recorded time on server is within 100ms which is assumed to be worst case clock skew
    configFileHistories.map(_.time.toEpochMilli).head shouldBe expectedRecordedTimeSpread
  }

  //DEOPSCSW-85 Record of time and date when config file is created/updated
  test("should record datetime when config file is updated") {
    val file       = Paths.get("/tmt/lgs/trombone/hcd.conf")
    val configData = ConfigData.fromString(configValue1)
    configService.create(file, configData, annex = false, "commit version: 1").await

    val updatedConfigData = ConfigData.fromString(configValue2)
    val now               = Instant.now()
    configService.update(file, updatedConfigData, "commit version: 2").await

    val configFileHistories = configService.history(file).await

    val expectedRecordedTimeSpread = now.toEpochMilli +- 100 //recorded time on server is within 100ms which is assumed to be worst case clock skew
    configFileHistories.map(_.time.toEpochMilli).head shouldBe expectedRecordedTimeSpread
  }

  // DEOPSCSW-45: Saving version information for config. file
  // DEOPSCSW-76: Access a list of all the versions of a stored configuration file
  // DEOPSCSW-63: Add comment while creating or updating a configuration file
  // DEOPSCSW-83: Retrieve file based on range of time for being most recent version
  test("should get the history of a file") {
    val file = Paths.get("/tmt/lgs/trombone/hcd.conf")

    intercept[FileNotFound] {
      configService.history(file).await
    }

    val commitMsg1 = "commit version: 1"
    val commitMsg2 = "commit version: 2"
    val commitMsg3 = "commit version: 3"

    val configId1 = configService.create(file, ConfigData.fromString(configValue1), annex = false, commitMsg1).await
    val createTS  = Instant.now

    val configId2 = configService.update(file, ConfigData.fromString(configValue2), commitMsg2).await
    val updateTS1 = Instant.now

    val configId3 = configService.update(file, ConfigData.fromString(configValue3), commitMsg3).await
    val updateTS2 = Instant.now

    // verify history without any parameter
    val configFileHistories = configService.history(file).await
    configFileHistories.size shouldBe 3
    configFileHistories.map(_.id) shouldBe List(configId3, configId2, configId1)
    configFileHistories.map(_.comment) shouldBe List(commitMsg3, commitMsg2, commitMsg1)

    // verify history with max results parameter
    val configFileHistories1 = configService.history(file, maxResults = 2).await
    configFileHistories1.size shouldBe 2
    configFileHistories1.map(_.id) shouldBe List(configId3, configId2)
    configFileHistories1.map(_.comment) shouldBe List(commitMsg3, commitMsg2)

    // verify history from given timestamp
    val configFileHistoriesFrom = configService.history(file, from = updateTS1).await
    configFileHistoriesFrom.size shouldBe 1
    configFileHistoriesFrom.map(_.id) shouldBe List(configId3)

    // verify history till given timestamp
    val configFileHistoriesTo = configService.history(file, to = updateTS1).await
    configFileHistoriesTo.size shouldBe 2
    configFileHistoriesTo.map(_.id) shouldBe List(configId2, configId1)

    // verify history within range of given timestamps
    val configFileHistoriesFromTo = configService.history(file, createTS, updateTS2).await
    configFileHistoriesFromTo.size shouldBe 2
    configFileHistoriesFromTo.map(_.id) shouldBe List(configId3, configId2)
  }

  // DEOPSCSW-48: Store new configuration file in Config. service
  test("should list all the available config files") {
    val tromboneConfig = Paths.get("trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    //  Check that files to be added does not already exists in the repo and then add
    configService.list().await.foreach { fileInfo ⇒
      fileInfo.path should not be tromboneConfig
      fileInfo.path should not be assemblyConfig
    }

    //  Add files to repo
    val tromboneConfigId = configService
      .create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, tromboneConfigComment)
      .await
    val assemblyConfigId = configService
      .create(assemblyConfig, ConfigData.fromString(configValue2), annex = false, assemblyConfigComment)
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

    configService.create(textFile, ConfigData.fromString(configValue1), annex = false, "commit config file").await
    configService.exists(textFile).await shouldBe true

    val binaryFile = Paths.get("/tmt/binary/hcd/ref.bin")

    configService.create(binaryFile, ConfigData.fromString(configValue1), annex = true, "commit config file").await
    configService.exists(binaryFile).await shouldBe true

  }

  // DEOPSCSW-91: Delete a given path
  test("should able to delete existing file") {
    val file = Paths.get("tromboneHCD.conf")
    configService
      .create(file, ConfigData.fromString(configValue1), annex = false, "commit trombone config file")
      .await

    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    configService.delete(file, "no longer needed").await
    configService.getLatest(file).await shouldBe None
  }

  // DEOPSCSW-91: Delete a given path
  test("deleting non existing file should throw FileNotFoundException") {
    val file = Paths.get("tromboneHCD.conf")
    intercept[FileNotFound] {
      configService.delete(file, "not needed").await
    }
  }

  // DEOPSCSW-91: Delete a given path
  test("delete removes all versions of a file") {
    val file = Paths.get("/a/b/csw.conf")

    configService.create(file, ConfigData.fromString(configValue1), annex = false, "commit config file").await
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    val configId2 = configService.update(file, ConfigData.fromString(configValue2), "updated config to assembly").await
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), "updated config to assembly").await

    // delete just removes the latest version of file by keeping the older versions
    // history call gives file not found exception as file is deleted
    // but you will still able to retrieve file with older versions
    configService.history(file).await.size shouldBe 3
    configService.delete(file, "no longer needed").await
    intercept[FileNotFound] {
      configService.history(file).await.size shouldBe 0
    }

    configService.getById(file, configId2).await.get.toStringF.await shouldBe configValue2
    configService.getById(file, configId3).await.get.toStringF.await shouldBe configValue3
    configService.getLatest(file).await shouldBe None
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  // DEOPSCSW-79: Reset the default version of a configuration file
  // DEOPSCSW-139: Provide new routes to get/set the current active version of the file
  test("should able to get, set and reset the active version of config file") {
    // create file
    val file = Paths.get("/tmt/test/setactive/getactive/resetactive/active.conf")
    configService.create(file, ConfigData.fromString(configValue1), annex = false, "create").await

    // verify that create does setActive to new version
    configService.getActive(file).await.get.toStringF.await shouldBe configValue1

    // Test latest is good also
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue1

    // update file twice
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), "Update 1").await
    val configId4 = configService.update(file, ConfigData.fromString(configValue4), "Update 2").await

    // check that get file without ID returns latest file
    configService.getLatest(file).await.get.toStringF.await shouldBe configValue4

    // check that getActive call before any setActive call should return the file with id with which it was created
    configService.getActive(file).await.get.toStringF.await shouldBe configValue1

    // set active version of file to id=3 and check get active returns same
    configService.setActiveVersion(file, configId3, "Setting active version for the first time").await
    configService.getActive(file).await.get.toStringF.await shouldBe configValue3
    configService.getActiveVersion(file).await.get shouldBe configId3

    // reset active version and check that get active returns latest version
    configService.resetActiveVersion(file, "resetting active version").await
    configService.getActive(file).await.get.toStringF.await shouldBe configValue4
    configService.getActiveVersion(file).await.get shouldBe configId4

    // update file and check get active returns last active version and not the latest version
    configService.update(file, ConfigData.fromString(configValue5), "Update 3").await
    configService.getActiveVersion(file).await.get shouldBe configId4
  }

  //DEOPSCSW-86: Retrieve a version of a configuration file based on time range for being default version
  test("should able to get history of active versions of file") {
    val file = Paths.get("/tmt/test/setactive/getactive/resetactive/active.conf")
    // on create call, active file gets created with this comment by default, note that this is an internal implementation
    val createActiveComment = "initializing active file with the first version"

    // create file
    val configId1      = configService.create(file, ConfigData.fromString(configValue1), annex = false, "create").await
    val createActiveTS = Instant.now()

    // update file twice
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), "Update 1").await
    val configId4 = configService.update(file, ConfigData.fromString(configValue4), "Update 2").await

    // set active version of file to id=3
    val setActiveComment = "Setting active version for the first time"
    configService.setActiveVersion(file, configId3, setActiveComment).await
    val setActiveTS = Instant.now()

    // reset active version and check that get active returns latest version
    val resetActiveComment1 = "resetting active version"
    configService.resetActiveVersion(file, resetActiveComment1).await
    val resetActiveTS1 = Instant.now()

    // update file and check get active returns last active version and not the latest version
    val configId5 = configService.update(file, ConfigData.fromString(configValue5), "Update 3").await
    configService.getActiveVersion(file).await.get shouldBe configId4

    val resetActiveComment2 = "resetting active version"
    configService.resetActiveVersion(file, resetActiveComment2).await
    val resetActiveTS2 = Instant.now()

    // verify complete history of active file without any parameters
    val completeHistory = configService.historyActive(file).await
    completeHistory.size shouldBe 4
    completeHistory.map(_.id) shouldBe List(configId5, configId4, configId3, configId1)
    completeHistory.map(_.comment) shouldBe List(resetActiveComment2, resetActiveComment1, setActiveComment, createActiveComment)

    // verify history of active file with max results parameter
    configService.historyActive(file, maxResults = 2).await.size shouldBe 2

    // verify history of active file from given timestamp
    val historyFrom = configService.historyActive(file, createActiveTS).await
    historyFrom.map(h ⇒ (h.id, h.comment)) shouldBe List((configId5, resetActiveComment2),
                                                         (configId4, resetActiveComment1),
                                                         (configId3, setActiveComment))

    // verify history of active file till given timestamp
    val historyTo = configService.historyActive(file, to = resetActiveTS1).await
    historyTo.map(h ⇒ (h.id, h.comment)) shouldBe List((configId4, resetActiveComment1),
                                                       (configId3, setActiveComment),
                                                       (configId1, createActiveComment))

    // verify history of active file within given timestamps
    val historyWithin = configService.historyActive(file, setActiveTS, resetActiveTS2).await
    historyWithin.map(h ⇒ (h.id, h.comment)) shouldBe List((configId5, resetActiveComment2), (configId4, resetActiveComment1))
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-79: Reset the default version of a configuration file
  // DEOPSCSW-139: Provide new routes to get/set the current active version of the file
  test("getActive and getActiveVersion should return None if file does not exists") {
    val file = Paths.get("/tmt/test/ahgvfyfgpp.conf")

    configService.getActive(file).await shouldBe None

    configService.getActiveVersion(file).await shouldBe None
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-79: Reset the default version of a configuration file
  // DEOPSCSW-139: Provide new routes to get/set the current active version of the file
  test("set and reset active should throw FileNotFound exception if file or version does not exists") {
    val file = Paths.get("/tmt/test/temp.conf")

    intercept[FileNotFound] {
      configService.resetActiveVersion(file, "resetting active version").await
    }

    configService.create(file, ConfigData.fromString(configValue1), annex = false, "test file").await

    intercept[FileNotFound] {
      configService.setActiveVersion(file, ConfigId(3), "setting active version").await
    }
  }

  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  test("should be able to store and retrieve file from annex store") {
    val file    = Paths.get("SomeAnnexFile.txt")
    val content = "testing annex file"

    val configData = ConfigData.fromString(content)

    //verify that files smaller than annex-min-file-size go to annex if flag is set
    serverWiring.settings.`annex-min-file-size` > configData.length shouldBe true

    val configId    = configService.create(file, configData, annex = true, "committing annex file").await
    val fileContent = configService.getById(file, configId).await.get
    fileContent.toStringF.await shouldBe content

    //Note that configService instance from the server-wiring can be used for assert-only calls for sha files
    //This call is invalid from client side
    val svnConfigData =
      serverWiring.configService
        .getById(Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}"), configId)
        .await
        .get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  // DEOPSCSW-135: Validation of suffix for active and sha files
  test("should list files from annex store without .$sha1 suffix") {
    val file1    = Paths.get("AnnexFile1.txt")
    val comment1 = "committing annex file"

    val file2    = Paths.get("AnnexFile2.txt")
    val comment2 = "committing one more annex file"

    val configId1 =
      configService.create(file1, ConfigData.fromString("testing annex file"), annex = true, comment1).await
    val configId2 =
      configService.create(file2, ConfigData.fromString("testing annex file"), annex = true, comment2).await

    val listOfFileInfo: List[ConfigFileInfo] = configService.list().await

    listOfFileInfo.toSet shouldBe Set(
      ConfigFileInfo(file1, configId1, comment1),
      ConfigFileInfo(file2, configId2, comment2)
    )
  }

  // DEOPSCSW-49: Update an Existing File with a New Version
  // DEOPSCSW-83: Retrieve file based on range of time for being most recent version
  test("should be able to update and retrieve the history of a file in annex store") {
    val file = Paths.get("SomeAnnexFile.txt")

    val comment1    = "create"
    val configData1 = ConfigData.fromString(configValue1)
    val configId1   = configService.create(file, configData1, annex = true, comment1).await
    val createTS1   = Instant.now

    val comment2    = "update 1"
    val configData2 = ConfigData.fromString(configValue2)
    val configId2   = configService.update(file, configData2, comment2).await
    val updateTS2   = Instant.now

    val comment3    = "update 2"
    val configData3 = ConfigData.fromString(configValue3)
    val configId3   = configService.update(file, configData3, comment3).await
    val updateTS3   = Instant.now

    configService.getById(file, configId1).await.get.toStringF.await shouldBe configValue1
    configService.getById(file, configId2).await.get.toStringF.await shouldBe configValue2
    configService.getById(file, configId3).await.get.toStringF.await shouldBe configValue3

    val shaFilePath = Paths.get(s"${file.toString}${serverWiring.settings.`sha1-suffix`}")
    //Note that configService instance from the server-wiring can be used for assert-only calls for sha files
    //This call is invalid from client side
    val shaOfConfigData1 = serverWiring.configService.getById(shaFilePath, configId1).await.get
    shaOfConfigData1.toStringF.await shouldBe Sha1.fromConfigData(configData1).await

    val shaOfConfigData2 = serverWiring.configService.getById(shaFilePath, configId2).await.get
    shaOfConfigData2.toStringF.await shouldBe Sha1.fromConfigData(configData2).await

    val shaOfConfigData3 = serverWiring.configService.getById(shaFilePath, configId3).await.get
    shaOfConfigData3.toStringF.await shouldBe Sha1.fromConfigData(configData3).await

    // verify history without any parameter
    val fileHistories: List[ConfigFileRevision] = configService.history(file).await
    fileHistories.map(history => (history.id, history.comment)) shouldBe List(
      (configId3, comment3),
      (configId2, comment2),
      (configId1, comment1)
    )

    // verify history with max results parameter
    configService.history(file, maxResults = 2).await.size shouldBe 2

    // verify history from given timestamp
    configService.history(file, createTS1).await.map(history ⇒ (history.id, history.comment)) shouldBe List(
      (configId3, comment3),
      (configId2, comment2)
    )

    // verify history till given timestamp
    configService.history(file, to = createTS1).await.map(history ⇒ (history.id, history.comment)) shouldBe List(
      (configId1, comment1)
    )

    // verify history within given range of timestamp
    configService.history(file, updateTS2, updateTS3).await.map(history ⇒ (history.id, history.comment)) shouldBe List(
      (configId3, comment3)
    )
  }

  //DEOPSCSW-86: Retrieve a version of a configuration file based on time range for being default version
  test("should able to get history of active versions of file from annex store") {
    val file = Paths.get("/tmt/annex/hcd.bin")
    // on create call, active file gets created with this comment by default, note that this is an internal implementation
    val createActiveComment = "initializing active file with the first version"

    // create file
    val configId1      = configService.create(file, ConfigData.fromString(configValue1), annex = true, "create").await
    val createActiveTS = Instant.now()

    // update file twice
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), "Update 1").await
    val configId4 = configService.update(file, ConfigData.fromString(configValue4), "Update 2").await

    // set active version of file to configId3
    val setActiveComment = "Setting active version for the first time"
    configService.setActiveVersion(file, configId3, setActiveComment).await
    val setActiveTS = Instant.now()

    // reset active version and check that get active returns latest version
    val resetActiveComment = "resetting active version"
    configService.resetActiveVersion(file, resetActiveComment).await
    val resetActiveTS = Instant.now()

    // verify complete history of active file without any parameters
    val completeHistory = configService.historyActive(file).await
    completeHistory.size shouldBe 3
    completeHistory.map(_.id) shouldBe List(configId4, configId3, configId1)
    completeHistory.map(_.comment) shouldBe List(resetActiveComment, setActiveComment, createActiveComment)

    // verify history of active file with max results parameter
    configService.historyActive(file, maxResults = 2).await.size shouldBe 2

    // verify history of active file from given timestamp
    val historyFrom = configService.historyActive(file, createActiveTS).await
    historyFrom.map(h ⇒ (h.id, h.comment)) shouldBe List((configId4, resetActiveComment), (configId3, setActiveComment))

    // verify history of active file till given timestamp
    val historyTo = configService.historyActive(file, to = resetActiveTS).await
    historyTo.map(h ⇒ (h.id, h.comment)) shouldBe List((configId4, resetActiveComment),
                                                       (configId3, setActiveComment),
                                                       (configId1, createActiveComment))

    // verify history of active file within given timestamps
    val historyWithin = configService.historyActive(file, setActiveTS, resetActiveTS).await
    historyWithin.map(h ⇒ (h.id, h.comment)) shouldBe List((configId4, resetActiveComment))
  }

  // DEOPSCSW-77: Set default version of configuration file in config service
  // DEOPSCSW-78: Get the default version of a configuration file
  // DEOPSCSW-70: Retrieve the current/most recent version of an existing configuration file
  test("should be able to get active file from annex store") {
    val file    = Paths.get("SomeAnnexFile.txt")
    val content = "testing annex file"
    val configId =
      configService.create(file, ConfigData.fromString(content), annex = true, "committing annex file").await

    configService.setActiveVersion(file, configId, "setting active version").await

    val newContent = "testing annex file, again"
    val newComment = "Updating file"
    configService.update(file, ConfigData.fromString(newContent), newComment).await

    val activeConfigData: ConfigData = configService.getActive(file).await.get
    activeConfigData.toStringF.await shouldBe content

    configService.resetActiveVersion(file, "resetting active version to latest").await

    val resetActiveConfigData: ConfigData = configService.getActive(file).await.get
    resetActiveConfigData.toStringF.await shouldBe newContent

    configService.delete(file, "deleting file").await

    val fileExists = configService.exists(file).await
    fileExists shouldBe false

    val activeAfterDelete = configService.getActive(file).await
    activeAfterDelete shouldBe None

    intercept[FileNotFound] {
      configService.resetActiveVersion(file, "resetting active version to latest").await
    }
  }

  test("should be able to get time stamped file from annex store") {
    val initialTime = Instant.MIN

    val file    = Paths.get("SomeAnnexFile.txt")
    val content = "testing annex file"
    configService.create(file, ConfigData.fromString(content), annex = true, "committing file to annex store").await

    val time = Instant.now()

    val newContent = "testing annex file, again"
    val newComment = "Updating file"
    configService.update(file, ConfigData.fromString(newContent), newComment).await

    val initialData = configService.getByTime(file, initialTime).await.get
    initialData.toStringF.await shouldBe content

    val oldTimeStampedData = configService.getByTime(file, time).await.get
    oldTimeStampedData.toStringF.await shouldBe content

    val latestData = configService.getByTime(file, Instant.now()).await.get
    latestData.toStringF.await shouldBe newContent

    configService.delete(file, "deleting file").await

    val fileExists = configService.exists(file).await
    fileExists shouldBe false

    val fileTimeStampedAfterDelete = configService.getLatest(file).await
    fileTimeStampedAfterDelete shouldBe None
  }

  test("should exclude .$active files from list") {
    val tromboneConfig = Paths.get("trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    // Check that files to be added does not already exists in the repo and then add
    configService.list().await.foreach { fileInfo ⇒
      fileInfo.path should not be tromboneConfig
      fileInfo.path should not be assemblyConfig
    }

    // Add files to repo
    val tromboneConfigId = configService
      .create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, tromboneConfigComment)
      .await
    val assemblyConfigId = configService
      .create(assemblyConfig, ConfigData.fromString(configValue2), annex = true, assemblyConfigComment)
      .await

    configService.setActiveVersion(tromboneConfig, tromboneConfigId, "setting active version").await
    configService.setActiveVersion(assemblyConfig, assemblyConfigId, "setting active version").await

    // list files from repo and assert that it contains added files
    val configFiles = configService.list().await

    configFiles.toSet shouldBe Set(
      ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment),
      ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)
    )
  }

  // DEOPSCSW-27: Storing binary component configurations
  // DEOPSCSW-81: Storing large files in the configuration service
  // DEOPSCSW-131: Detect and handle oversize files
  test("should be able to store and retrieve text file from annex store when size is greater than configured size",
       FileSystemSensitive) {
    val fileName              = "tromboneContainer.conf"
    val path                  = Paths.get(getClass.getClassLoader.getResource(fileName).toURI)
    val configData            = ConfigData.fromPath(path)
    val config: Config        = ConfigFactory.parseString("csw-config-server.annex-min-file-size=1 KiB")
    val serverWiringAnnexTest = ServerWiring.make(config)

    val configId =
      serverWiringAnnexTest.configService
        .create(Paths.get(fileName), configData, annex = false, s"committing file: $fileName")
        .await

    // This test verifies that the binary file has been stored properly in the configuration service
    val expectedContent =
      serverWiringAnnexTest.configService.getById(Paths.get(fileName), configId).await.get.toInputStream.toByteArray
    val diskFile = getClass.getClassLoader.getResourceAsStream(fileName)
    expectedContent shouldBe diskFile.toByteArray

    // This test verifies that the file was stored in the annex because the file with the sha1-suffix only exists if in the annex
    val svnConfigData =
      serverWiringAnnexTest.configService
        .getById(Paths.get(s"$fileName${serverWiring.settings.`sha1-suffix`}"), configId)
        .await
        .get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
    serverWiringAnnexTest.actorRuntime.shutdown(UnknownReason).await
  }

  //DEOPSCSW-75 List the names of configuration files that match a path
  test("should list all files in a repository") {
    val tromboneConfig        = Paths.get("a/c/trombone.conf")
    val hcdConfig             = Paths.get("a/b/c/hcd/hcd.conf")
    val assemblyBinaryConfig1 = Paths.get("a/b/assembly/assembly1.fits")
    val assemblyBinaryConfig2 = Paths.get("a/b/c/assembly/assembly2.fits")

    configService.create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, "hello trombone").await
    configService
      .create(assemblyBinaryConfig1, ConfigData.fromString(configValue2), annex = true, "hello assembly1")
      .await
    configService
      .create(assemblyBinaryConfig2, ConfigData.fromString(configValue2), annex = true, "hello assembly2")
      .await
    configService.create(hcdConfig, ConfigData.fromString(configValue3), annex = false, "hello hcd").await

    val list = configService.list().await
    list.map(_.path).toSet shouldBe Set(tromboneConfig, assemblyBinaryConfig1, assemblyBinaryConfig2, hcdConfig)

  }

  //DEOPSCSW-75 List the names of configuration files that match a path
  test("should give empty list if pattern does not match any file") {
    val tromboneConfig = Paths.get("a/c/trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")
    val hcdConfig      = Paths.get("a/b/c/hcd/hcd.conf")

    configService.create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, "hello trombone").await
    configService.create(assemblyConfig, ConfigData.fromString(configValue2), annex = true, "hello assembly").await
    configService.create(hcdConfig, ConfigData.fromString(configValue3), annex = false, "hello hcd").await

    val fileInfoes3 = configService.list(pattern = Some("a/b/c/d.*")).await
    fileInfoes3.isEmpty shouldBe true
  }

  //DEOPSCSW-75 List the names of configuration files that match a path
  test("should filter list based on the pattern") {
    val tromboneConfig = Paths.get("a/c/trombone.conf")
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf")
    val hcdConfig      = Paths.get("a/b/c/hcd/hcd.conf")

    configService.create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, "hello trombone").await
    configService.create(assemblyConfig, ConfigData.fromString(configValue2), annex = true, "hello assembly").await
    configService.create(hcdConfig, ConfigData.fromString(configValue3), annex = false, "hello hcd").await

    val fileInfoes1 = configService.list(pattern = Some("a/b.*")).await
    fileInfoes1.map(_.path).toSet shouldBe Set(assemblyConfig, hcdConfig)

    val fileInfoes2 = configService.list(pattern = Some(".*.conf")).await
    fileInfoes2.map(_.path).toSet shouldBe Set(tromboneConfig, assemblyConfig, hcdConfig)

    val fileInfoes4 = configService.list(pattern = Some("a/b/c.*")).await
    fileInfoes4.map(_.path).toSet shouldBe Set(hcdConfig)

    val fileInfoes6 = configService.list(pattern = Some(".*hcd.*")).await
    fileInfoes6.map(_.path).toSet shouldBe Set(hcdConfig)
  }

  //DEOPSCSW-132 List oversize and normal sized files
  //DEOPSCSW-75 List the names of configuration files that match a path
  test("should filter list based on the type and pattern") {
    val tromboneConfig   = Paths.get("a/c/trombone.conf")
    val hcdConfig        = Paths.get("a/b/c/hcd/hcd.conf")
    val assemblyBinConf1 = Paths.get("a/b/assembly/assembly1.fits")
    val assemblyBinConf2 = Paths.get("a/b/c/assembly/assembly2.fits")

    configService.create(tromboneConfig, ConfigData.fromString(configValue1), annex = false, "hello trombone").await
    configService.create(assemblyBinConf1, ConfigData.fromString(configValue2), annex = true, "hello assembly1").await
    configService.create(assemblyBinConf2, ConfigData.fromString(configValue2), annex = true, "hello assembly2").await
    configService.create(hcdConfig, ConfigData.fromString(configValue3), annex = false, "hello hcd").await

    // list all annex files
    val fileInfoes1 = configService.list(Some(FileType.Annex)).await
    fileInfoes1.map(_.path).toSet shouldBe Set(assemblyBinConf1, assemblyBinConf2)

    // list all normal files
    val fileInfoes2 = configService.list(Some(FileType.Normal)).await
    fileInfoes2.map(_.path).toSet shouldBe Set(tromboneConfig, hcdConfig)

    // list all annex files located at path : a/b/c/
    val fileInfoes3 = configService.list(Some(FileType.Annex), Some("a/b/c/.*")).await
    fileInfoes3.map(_.path).toSet shouldBe Set(assemblyBinConf2)

    // list all annex files with extension : .fits
    val fileInfoes4 = configService.list(Some(FileType.Annex), Some(".*.fits")).await
    fileInfoes4.map(_.path).toSet shouldBe Set(assemblyBinConf1, assemblyBinConf2)

    // list all annex files which contains assembly substring
    val fileInfoes5 = configService.list(Some(FileType.Annex), Some(".*assembly.*")).await
    fileInfoes5.map(_.path).toSet shouldBe Set(assemblyBinConf1, assemblyBinConf2)

    // list all normal files located at path : a/b/c/
    val fileInfoes6 = configService.list(Some(FileType.Normal), Some("a/b/c/.*")).await
    fileInfoes6.map(_.path).toSet shouldBe Set(hcdConfig)

    // list all normal files with extension : .conf
    val fileInfoes7 = configService.list(Some(FileType.Normal), Some(".*.conf")).await
    fileInfoes7.map(_.path).toSet shouldBe Set(tromboneConfig, hcdConfig)

    // list all normal files which contains hcd substring
    val fileInfoes8 = configService.list(Some(FileType.Normal), Some(".*hcd.*")).await
    fileInfoes8.map(_.path).toSet shouldBe Set(hcdConfig)
  }

  //DEOPSCSW-140 Provide new routes to get active file as of date
  test("should get the correct active version of the file based on time") {

    // create file
    val file = Paths.get("/tmt/test/setactive/getactive/resetactive/active.conf")
    configService.create(file, ConfigData.fromString(configValue1), annex = false, "hello world").await

    // update file twice
    val configId = configService.update(file, ConfigData.fromString(configValue2), "Updated config to assembly").await
    configService.update(file, ConfigData.fromString(configValue3), "Updated config to assembly").await

    val tHeadRevision = Instant.now()
    configService.setActiveVersion(file, configId, "Setting active version for the first time").await

    val tActiveRevision1 = Instant.now()

    configService.resetActiveVersion(file, "resetting active version").await

    configService.getActiveByTime(file, tHeadRevision).await.get.toStringF.await shouldBe configValue1
    configService.getActiveByTime(file, tActiveRevision1).await.get.toStringF.await shouldBe configValue2
    configService.getActiveByTime(file, Instant.now()).await.get.toStringF.await shouldBe configValue3
  }

  //DEOPSCSW-133: Provide meta config for normal and oversize repo
  test("should get metadata") {
    val config: Config = ConfigFactory.parseString("""
    |csw-config-server.repository-dir=/test/csw-config-svn
    |csw-config-server.annex-dir=/test/csw-config-temp
    |csw-config-server.annex-min-file-size=333 MiB
    |akka.http.server.parsing.max-content-length=500 MiB
      """.stripMargin)

    val serverWiringMetadataTest = ServerWiring.make(config)

    val metadata = serverWiringMetadataTest.configService.getMetadata.await
    metadata.repoPath shouldBe "/test/csw-config-svn"
    metadata.annexPath shouldBe "/test/csw-config-temp"
    metadata.annexMinFileSize shouldBe "333 MiB"
    metadata.maxConfigFileSize shouldBe "500 MiB"
    serverWiringMetadataTest.actorRuntime.shutdown(UnknownReason).await
  }
}
