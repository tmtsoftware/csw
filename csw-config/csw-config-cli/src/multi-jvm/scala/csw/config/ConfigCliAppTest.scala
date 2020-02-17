package csw.config

import java.nio.file.{Files, Paths}

import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.ConfigFactory
import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.commons.ResourceReader
import csw.config.api.ConfigData
import csw.config.cli.wiring.Wiring
import csw.config.client.internal.ActorRuntime
import csw.config.client.scaladsl.ConfigClientFactory
import csw.config.helpers.TwoClientsAndServer
import csw.config.server.commons.TestFileUtils
import csw.config.server.mocks.MockedAuthentication
import csw.config.server.{ServerWiring, Settings}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.helpers.LSNodeSpec
import csw.location.server.http.MultiNodeHTTPLocationService
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuiteLike

class ConfigCliAppTestMultiJvmNode1 extends ConfigCliAppTest(0)
class ConfigCliAppTestMultiJvmNode2 extends ConfigCliAppTest(0)
class ConfigCliAppTestMultiJvmNode3 extends ConfigCliAppTest(0)

// DEOPSCSW-43: Access Configuration service from any CSW component
class ConfigCliAppTest(ignore: Int)
    extends LSNodeSpec(config = new TwoClientsAndServer, mode = "http")
    with MultiNodeHTTPLocationService
    with AnyFunSuiteLike
    with MockedAuthentication
    with MockitoSugar {

  import config._

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  private val nativeAuthAdapter: InstalledAppAuthAdapter = mock[InstalledAppAuthAdapter]

  private val (inputFilePath, inputFileContents)               = ResourceReader.readAndCopyToTmp("/tromboneHCDContainer.conf")
  private val (updatedInputFilePath, updatedInputFileContents) = ResourceReader.readAndCopyToTmp("/tromboneAssembly.conf")

  private val repoPath1 = "/client1/hcd/text/tromboneHCDContainer.conf"
  private val repoPath2 = "/client2/hcd/text/app.conf"
  private val comment   = "test comment"

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
  }

  test("should upload, update, get and set active version of configuration files") {
    runOn(server) {
      // Start server on first node
      val serverWiring = ServerWiring.make(locationService, securityDirectives)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await
      enterBarrier("server-started")
      enterBarrier("client1-create")
      enterBarrier("client2-create-pass")
      enterBarrier("client1-update")
      enterBarrier("client2-update-pass")
      enterBarrier("client1-setActive")
      enterBarrier("client2-create")
    }

    // config client command line app is exercised on client1
    runOn(client1) {
      enterBarrier("server-started")

      def cliApp() = Wiring.noPrinting(HttpLocationServiceFactory.makeLocalClient, factory, nativeAuthAdapter).cliApp

      cliApp().start("csw-config-cli", Array("create", repoPath1, "-i", inputFilePath.toString, "-c", comment))
      enterBarrier("client1-create")
      enterBarrier("client2-create-pass")

      cliApp().start("csw-config-cli", Array("update", repoPath1, "-i", updatedInputFilePath.toString, "-c", comment))
      enterBarrier("client1-update")
      enterBarrier("client2-update-pass")

      cliApp().start("csw-config-cli", Array("setActiveVersion", repoPath1, "--id", "1", "-c", comment))
      enterBarrier("client1-setActive")

      // Verify that client1 (cli app) is able to access file created by client2
      enterBarrier("client2-create")
      val tempOutputFile = Files.createTempFile("output", ".conf").toString
      cliApp().start("csw-config-cli", Array("get", repoPath2, "-o", tempOutputFile))
      Files.readString(Paths.get(tempOutputFile)) shouldEqual inputFileContents

    }

    // config client admin api is exercised on client2
    runOn(client2) {
      enterBarrier("server-started")
      val actorRuntime  = new ActorRuntime(system.toTyped)
      val configService = ConfigClientFactory.adminApi(system.toTyped, locationService, factory)

      enterBarrier("client1-create")
      val actualConfigValue = configService.getLatest(Paths.get(repoPath1)).await.get.toStringF(actorRuntime.typedSystem).await
      actualConfigValue shouldBe inputFileContents
      enterBarrier("client2-create-pass")

      enterBarrier("client1-update")
      val actualUpdatedConfigValue =
        configService.getLatest(Paths.get(repoPath1)).await.get.toStringF(actorRuntime.typedSystem).await
      actualUpdatedConfigValue shouldBe updatedInputFileContents
      enterBarrier("client2-update-pass")

      enterBarrier("client1-setActive")
      val actualActiveConfigValue =
        configService.getActive(Paths.get(repoPath1)).await.get.toStringF(actorRuntime.typedSystem).await
      actualActiveConfigValue shouldBe inputFileContents

      configService
        .create(Paths.get(repoPath2), ConfigData.fromString(inputFileContents), comment = "creating app.conf")
        .await
      enterBarrier("client2-create")
    }
    enterBarrier("after-1")
  }
}
