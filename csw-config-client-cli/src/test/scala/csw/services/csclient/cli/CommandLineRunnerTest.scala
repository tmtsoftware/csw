package csw.services.csclient.cli

import java.nio.file.{Files, Paths}

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
    Files.delete(Paths.get(outputFilePath))
  }

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    wiring.actorRuntime.shutdown().await
    Files.delete(Paths.get(inputFilePath))
    Files.delete(Paths.get(updatedInputFilePath))
  }

  test("should able to create a file in repo and read it from repo to local disk") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.run(parsedCreateArgs.get)

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getLatestArgs)
    commandLineRunner.run(parsedGetArgs.get)

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to update, delete and check for existence of a file from repo") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.run(parsedCreateArgs.get)

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    commandLineRunner.run(parsedUpdateArgs.get)

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getLatestArgs)
    commandLineRunner.run(parsedGetArgs.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    //  file should exist
    //  is there any way to assert here?
    val parsedExistsArgs1: Option[Options] = ArgsParser.parse(existsArgs)
    commandLineRunner.run(parsedExistsArgs1.get)

    //  delete file
    val parsedDeleteArgs: Option[Options] = ArgsParser.parse(deleteArgs)
    commandLineRunner.run(parsedDeleteArgs.get)

    //  deleted file should not exist
    //  is there any way to assert here?
    val parsedExistsArgs: Option[Options] = ArgsParser.parse(existsArgs)
    commandLineRunner.run(parsedExistsArgs.get)
  }

  test("should able to set, reset and get the default version of file.") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    commandLineRunner.run(parsedCreateArgs.get)

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    commandLineRunner.run(parsedUpdateArgs.get)

    //  set default version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetDefaultArgs: Option[Options] = ArgsParser.parse(setDefaultAllArgs)
    commandLineRunner.run(parsedSetDefaultArgs.get)

    //  get default version of file and store it at location: /tmp/output.txt
    val parsedGetDefaultArgs: Option[Options] = ArgsParser.parse(getMinimalArgs)
    commandLineRunner.run(parsedGetDefaultArgs.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    //  reset default version of file and store it at location: /tmp/output.txt
    val parsedResetDefaultArgs: Option[Options] = ArgsParser.parse(resetDefaultAllArgs)
    commandLineRunner.run(parsedResetDefaultArgs.get)

    //  get default version of file and store it at location: /tmp/output.txt
    commandLineRunner.run(parsedGetDefaultArgs.get)

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents
  }
}
