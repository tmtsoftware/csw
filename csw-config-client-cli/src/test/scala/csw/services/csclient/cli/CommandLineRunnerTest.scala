package csw.services.csclient.cli

import java.nio.file.{Files, Paths}
import java.time.Instant

import csw.services.config.api.models.ConfigId
import csw.services.config.server.ServerWiring
import csw.services.csclient.commons.TestFutureExtension.RichFuture
import csw.services.csclient.commons.{ArgsUtil, TestFileUtils}
import csw.services.location.commons.ClusterAwareSettings
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class CommandLineRunnerTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.onPort(3552))
  private val httpService  = serverWiring.httpService
  httpService.registeredLazyBinding.await

  private val wiring = new ClientCliWiring(ClusterAwareSettings.joinLocal(3552))
  import wiring.commandLineRunner

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  val ArgsUtil = new ArgsUtil
  import ArgsUtil._

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
    if (Files.exists(Paths.get(outputFilePath))) {
      Files.delete(Paths.get(outputFilePath))
    }
  }

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    wiring.actorRuntime.shutdown().await
    Files.delete(Paths.get(inputFilePath))
    Files.delete(Paths.get(updatedInputFilePath))
  }

  //DEOPSCSW-72: Retrieve a configuration file to a specified file location on a local disk
  test("should able to create a file in repo and read it from repo to local disk") {

    commandLineRunner.create(ArgsParser.parse(createMinimalArgs).get) shouldBe ConfigId(1)

    commandLineRunner.get(ArgsParser.parse(getLatestArgs).get) shouldBe Some(Paths.get(outputFilePath))

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to update, delete and check for existence of a file from repo") {

    commandLineRunner.create(ArgsParser.parse(createMinimalArgs).get)

    val getByDateArgs = Array("get", relativeRepoPath, "-o", outputFilePath, "--date", Instant.now.toString)

    val updateConfigId = commandLineRunner.update(ArgsParser.parse(updateAllArgs).get)

    commandLineRunner.get(ArgsParser.parse(getLatestArgs).get) shouldBe Some(Paths.get(outputFilePath))

    commandLineRunner.get(ArgsParser.parse(getByIdArgs :+ updateConfigId.id).get) shouldBe Some(
        Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    commandLineRunner.get(ArgsParser.parse(getByDateArgs).get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    //  file should exist
    val parsedExistsArgs1: Option[Options] = ArgsParser.parse(existsArgs)
    commandLineRunner.exists(parsedExistsArgs1.get) shouldBe true

    //  delete file
    val parsedDeleteArgs: Option[Options] = ArgsParser.parse(deleteArgs)
    commandLineRunner.delete(parsedDeleteArgs.get)

    //  deleted file should not exist
    val parsedExistsArgs: Option[Options] = ArgsParser.parse(existsArgs)
    commandLineRunner.exists(parsedExistsArgs.get) shouldBe false
  }

  test("should be able to list files and use filter pattern") {
    val normalFileName                                = "troubleshooting"
    val annexFileName                                 = "firmware"
    def relativeRepoPath(fileName: String, i: String) = s"path/hcd/$fileName$i.conf"

    //  create 4 normal files
    val normalFiles = for {
      i ← 1 to 4
    } yield relativeRepoPath(normalFileName, i.toString)
    normalFiles.map { fileName ⇒
      commandLineRunner.create(ArgsParser.parse(Array("create", fileName, "-i", inputFilePath)).get)
    }

    //  create 3 annex files
    val annexFiles = for {
      i ← 1 to 3
    } yield relativeRepoPath(annexFileName, i.toString)
    annexFiles.map { fileName ⇒
      commandLineRunner.create(ArgsParser.parse(Array("create", fileName, "-i", inputFilePath, "--annex")).get)
    }

    commandLineRunner.list(ArgsParser.parse(Array("list", "--annex", "--normal")).get) shouldBe empty

    commandLineRunner
      .list(ArgsParser.parse(Array("list", "--normal")).get)
      .map(_.path.toString)
      .toSet shouldBe normalFiles.toSet

    commandLineRunner
      .list(ArgsParser.parse(Array("list", "--annex")).get)
      .map(_.path.toString)
      .toSet shouldBe annexFiles.toSet

    commandLineRunner
      .list(ArgsParser.parse(Array("list", "--pattern", "/path/hcd/*.*")).get)
      .map(_.path.toString)
      .toSet shouldBe (annexFiles ++ normalFiles).toSet
  }

  test("should able to set, reset and get the active version of file.") {

    commandLineRunner.create(ArgsParser.parse(createMinimalArgs).get)

    val updateConfigId = commandLineRunner.update(ArgsParser.parse(updateAllArgs).get)

    //  set active version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetActiveArgs: Option[Options] = ArgsParser.parse(setActiveAllArgs)
    commandLineRunner.setActiveVersion(parsedSetActiveArgs.get)

    val getByDateArgs =
      Array("getActiveByTime", relativeRepoPath, "-o", outputFilePath, "--date", Instant.now.toString)

    //  get active version of file and store it at location: /tmp/output.txt
    val parsedGetActiveArgs: Option[Options] = ArgsParser.parse(getMinimalArgs)
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get) shouldBe ConfigId(parsedSetActiveArgs.get.id.get)
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    commandLineRunner.resetActiveVersion(ArgsParser.parse(resetActiveAllArgs).get)

    //  get active version of file and store it at location: /tmp/output.txt
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get) shouldBe updateConfigId
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    commandLineRunner.getActiveByTime(ArgsParser.parse(getByDateArgs).get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to fetch history of file.") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    val createConfigId                    = commandLineRunner.create(parsedCreateArgs.get)

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    val updateConfigId                    = commandLineRunner.update(parsedUpdateArgs.get)

    //  update file content
    val parsedUpdate2Args: Option[Options] = ArgsParser.parse(updateAllArgs)
    val update2ConfigId                    = commandLineRunner.update(parsedUpdate2Args.get)

    commandLineRunner.history(ArgsParser.parse(historyArgs).get).map(_.id) shouldBe List(update2ConfigId,
      updateConfigId, createConfigId)
  }

  test("get repository MetaData from server") {
    val parsedMetaDataArgs: Option[Options] = ArgsParser.parse(meteDataArgs)
    val actualMetadata                      = commandLineRunner.getMetadata(parsedMetaDataArgs.get)
    actualMetadata.toString should not be empty
  }
}
