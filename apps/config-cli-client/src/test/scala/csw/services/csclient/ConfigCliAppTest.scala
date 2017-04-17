package csw.services.csclient

import java.io.File
import java.nio.file.{Files, Paths}

import csw.services.config.server.ServerWiring
import csw.services.csclient.commons.TestFileUtils
import csw.services.csclient.commons.TestFutureExtension.RichFuture
import csw.services.csclient.models.Options
import csw.services.csclient.utils.CmdLineArgsParser
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConfigCliAppTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val locationService = LocationServiceFactory.withSettings(ClusterSettings().onPort(3552))
  private val serverWiring = ServerWiring.make(locationService)
  private val httpService = serverWiring.httpService
  httpService.lazyBinding.await

  val ConfigCliApp = new ConfigCliApp(ClusterSettings().joinLocal(3552))

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import csw.services.csclient.commons.CmdLineArgsUtil._

  override protected def beforeEach(): Unit = {
    serverWiring.svnAdmin.initSvnRepo()
  }

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
    new File(outputFilePath).delete()
  }

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    ConfigCliApp.shutdown().await
    new File(inputFilePath).delete()
    new File(updatedInputFilePath).delete()
  }

  test("should able to create a file a in svn repo and read it from svn to local disk") {

    //  create file
    val parsedCreateArgs: Option[Options] = CmdLineArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = CmdLineArgsParser.parse(getMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedGetArgs.get).await

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    Files.exists(Paths.get(outputFilePath)) shouldEqual true
    val source = scala.io.Source.fromFile(outputFilePath)
    try source.mkString shouldEqual inputFileContents
    finally {
      source.close()
    }
  }

  test("should able to update, delete and check for existence of a file from svn repo") {

    //  create file
    val parsedCreateArgs: Option[Options] = CmdLineArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  update file content
    val parsedUpdateArgs: Option[Options] = CmdLineArgsParser.parse(updateAllArgs)
    ConfigCliApp.commandLineRunner(parsedUpdateArgs.get).await

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = CmdLineArgsParser.parse(getMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedGetArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    Files.exists(Paths.get(outputFilePath)) shouldEqual true
    val source = scala.io.Source.fromFile(outputFilePath)
    try source.mkString shouldEqual updatedInputFileContents
    finally {
      source.close()
    }

    //  file should exist
    //  is there any way to assert here?
    val parsedExistsArgs1: Option[Options] = CmdLineArgsParser.parse(existsArgs)
    ConfigCliApp.commandLineRunner(parsedExistsArgs1.get).await

    //  delete file
    val parsedDeleteArgs: Option[Options] = CmdLineArgsParser.parse(deleteArgs)
    ConfigCliApp.commandLineRunner(parsedDeleteArgs.get).await

    //  deleted file should not exist
    //  is there any way to assert here?
    val parsedExistsArgs: Option[Options] = CmdLineArgsParser.parse(existsArgs)
    ConfigCliApp.commandLineRunner(parsedExistsArgs.get).await
  }

  test("should able to set, reset and get the default version of file.") {

    //  create file
    val parsedCreateArgs: Option[Options] = CmdLineArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  update file content
    val parsedUpdateArgs: Option[Options] = CmdLineArgsParser.parse(updateAllArgs)
    ConfigCliApp.commandLineRunner(parsedUpdateArgs.get).await

    //  set default version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetDefaultArgs: Option[Options] = CmdLineArgsParser.parse(setDefaultAllArgs)
    ConfigCliApp.commandLineRunner(parsedSetDefaultArgs.get).await

    //  get default version of file and store it at location: /tmp/output.txt
    val parsedGetDefaultArgs: Option[Options] = CmdLineArgsParser.parse(getDefaultArgs)
    ConfigCliApp.commandLineRunner(parsedGetDefaultArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    Files.exists(Paths.get(outputFilePath)) shouldEqual true
    val source1 = scala.io.Source.fromFile(outputFilePath)
    try source1.mkString shouldEqual inputFileContents
    finally {
      source1.close()
    }

    //  reset default version of file and store it at location: /tmp/output.txt
    val parsedResetDefaultArgs: Option[Options] = CmdLineArgsParser.parse(setDefaultMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedResetDefaultArgs.get).await

    //  get default version of file and store it at location: /tmp/output.txt
    ConfigCliApp.commandLineRunner(parsedGetDefaultArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    Files.exists(Paths.get(outputFilePath)) shouldEqual true
    val source2 = scala.io.Source.fromFile(outputFilePath)
    try source2.mkString shouldEqual updatedInputFileContents
    finally {
      source2.close()
    }
  }
}
