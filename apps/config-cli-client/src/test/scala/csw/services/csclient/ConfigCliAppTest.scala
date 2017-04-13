package csw.services.csclient

import java.io.File
import java.nio.file.{Files, Paths}

import csw.services.csclient.commons.TestFileUtils
import csw.services.config.server.ServerWiring
import csw.services.csclient.commons.TestFutureExtension.RichFuture
import csw.services.csclient.models.Options
import csw.services.csclient.utils.CmdLineArgsParser
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class CustomServerWiring extends ServerWiring {
  override lazy val locationService: LocationService = {
    LocationServiceFactory.withSettings(ClusterSettings().onPort(3552))
  }
}

class ConfigCliAppTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  lazy val serverWiring = new CustomServerWiring
  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import csw.services.csclient.commons.CmdLineArgsUtil._

  override protected def beforeAll(): Unit = {
    //  start http server
    serverWiring.httpService.lazyBinding.await
  }

  override protected def beforeEach(): Unit = {
    serverWiring.svnAdmin.initSvnRepo()
  }

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
    new File(outputFilePath).delete()
  }

  override protected def afterAll(): Unit = {
    serverWiring.httpService.shutdown().await
    ConfigCliApp.shutdown()
    new File(inputFilePath).delete()
    new File(updatedInputFilePath).delete()
  }

  test("should able to create a file a in svn repo and read it from svn to local disk") {

    //  set the clusterSeeds system property for a client to join a same cluster as server
    System.setProperty("clusterSeeds", s"${ClusterAwareSettings.hostname}:3552")

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

    //  set the clusterSeeds system property for a client to join a same cluster as server
    System.setProperty("clusterSeeds", s"${ClusterAwareSettings.hostname}:3552")

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

}
