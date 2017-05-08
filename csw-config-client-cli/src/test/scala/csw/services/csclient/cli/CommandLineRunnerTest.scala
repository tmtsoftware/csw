package csw.services.csclient.cli

import java.nio.file.{Files, Paths}

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

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.create(parsedCreateArgs.get) shouldBe ConfigId(1)

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getLatestArgs)
    commandLineRunner.get(parsedGetArgs.get) shouldBe Some(parsedGetArgs.get.outputFilePath.get)

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to update, delete and check for existence of a file from repo") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.create(parsedCreateArgs.get)

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    val updateConfigId                    = commandLineRunner.update(parsedUpdateArgs.get)

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getLatestArgs)
    commandLineRunner.get(parsedGetArgs.get) shouldBe Some(parsedGetArgs.get.outputFilePath.get)

    val parsedGetByIdArgs: Option[Options] = ArgsParser.parse(getByIdArgs :+ updateConfigId.id)
    commandLineRunner.get(parsedGetByIdArgs.get) shouldBe Some(parsedGetByIdArgs.get.outputFilePath.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

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
    def relativeRepoPath(fileName: String, i: String) = s"/path/hcd/$fileName$i.conf"

    //  create 4 normal files
    for (i ← 1 to 4) {
      commandLineRunner
        .create(ArgsParser.parse(Array("create", relativeRepoPath(normalFileName, i.toString), "-i", inputFilePath)).get)
    }

    //  create 3 oversize files
    for (i ← 1 to 3) {
      commandLineRunner
        .create(ArgsParser.parse(Array("create", relativeRepoPath(annexFileName, i.toString), "-i", inputFilePath, "--annex")).get)
    }

    commandLineRunner.list(ArgsParser.parse(Array("list", "--annex", "--normal")).get) shouldBe empty
    commandLineRunner.list(ArgsParser.parse(Array("list", "--normal")).get).size shouldBe 4
    commandLineRunner.list(ArgsParser.parse(Array("list", "--annex")).get).size shouldBe 3
    commandLineRunner.list(ArgsParser.parse(Array("list", "--pattern", "/path/hcd/*.*")).get).size shouldBe 7
    commandLineRunner
      .list(ArgsParser.parse(Array("list", "--pattern", "/path/hcd/tr*.*")).get)
      .size shouldBe 4
    commandLineRunner.list(ArgsParser.parse(Array("list", "--pattern", "/path/hcd/firmware.*")).get).size shouldBe 3
  }

  test("should able to set, reset and get the active version of file.") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.create(parsedCreateArgs.get)

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    val updateConfigId                    = commandLineRunner.update(parsedUpdateArgs.get)

    //  set active version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetActiveArgs: Option[Options] = ArgsParser.parse(setActiveAllArgs)
    commandLineRunner.setActiveVersion(parsedSetActiveArgs.get)

    //  get active version of file and store it at location: /tmp/output.txt
    val parsedGetActiveArgs: Option[Options] = ArgsParser.parse(getMinimalArgs)
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get) shouldBe ConfigId(parsedSetActiveArgs.get.id.get)
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(parsedGetActiveArgs.get.outputFilePath.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    //  reset active version of file and store it at location: /tmp/output.txt
    val parsedResetActiveArgs: Option[Options] = ArgsParser.parse(resetActiveAllArgs)
    commandLineRunner.resetActiveVersion(parsedResetActiveArgs.get)

    //  get active version of file and store it at location: /tmp/output.txt
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get) shouldBe updateConfigId
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(parsedGetActiveArgs.get.outputFilePath.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents
  }

  test("get repository MetaData from server") {
    val parsedMetaDataArgs: Option[Options] = ArgsParser.parse(meteDataArgs)
    val actualMetadata                      = commandLineRunner.getMetadata(parsedMetaDataArgs.get)
    actualMetadata.toString should not be empty
  }
}
