package csw.services.csclient

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.config.api.models.ConfigData
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}
import csw.services.csclient.helpers.TwoClientsAndServer
import csw.services.location.commons.ClusterSettings
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import org.scalatest.FunSuiteLike

class ConfigCliTestMultiJvmNode1 extends ConfigCliTest(0)
class ConfigCliTestMultiJvmNode2 extends ConfigCliTest(0)
class ConfigCliTestMultiJvmNode3 extends ConfigCliTest(0)

class ConfigCliTest(ignore: Int) extends LSNodeSpec(config = new TwoClientsAndServer) with FunSuiteLike {

  import config._

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
  }

  val inputFilePath            = getClass.getResource("/tromboneHCD.conf").getPath
  val updatedInputFilePath     = getClass.getResource("/tromboneAssembly.conf").getPath
  val inputFileContents        = scala.io.Source.fromFile(inputFilePath).mkString
  val updatedInputFileContents = scala.io.Source.fromFile(updatedInputFilePath).mkString
  val repoPath1                = "/client1/hcd/text/tromboneHCD.conf"
  val repoPath2                = "/client2/hcd/text/app.conf"

  test("should upload, update, get and set active version of configuration files") {
    runOn(server) {
      // Start server on first node
      val serverWiring = ServerWiring.make(locationService)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await
      enterBarrier("server-started")
      enterBarrier("member1-create")
      enterBarrier("member1-update")
      enterBarrier("member1-setActive")
      enterBarrier("member2-create")
    }
    runOn(client1) {
      enterBarrier("server-started")

      // create file using cli app from client1 and verify client2 able to access it
      val cliMain    = new Main(ClusterSettings().joinLocal(3552))
      val createArgs = Array("create", repoPath1, "-i", inputFilePath)
      cliMain.start(createArgs)
      enterBarrier("member1-create")

      // update file using cli app from client1 and verify client2 able to access it
      val cliMain1   = new Main(ClusterSettings().joinLocal(3552))
      val updateArgs = Array("update", repoPath1, "-i", updatedInputFilePath)
      cliMain1.start(updateArgs)
      enterBarrier("member1-update")

      // set active version of file using cli app from client1 and verify client2 able to access it
      val cliMain2      = new Main(ClusterSettings().joinLocal(3552))
      val setActiveArgs = Array("setActive", repoPath1, "--id", "1")
      cliMain2.start(setActiveArgs)
      enterBarrier("member1-setActive")

      // Verify that client1 (cli app) is able to access file created by client2
      enterBarrier("member2-create")
      val cliMain3       = new Main(ClusterSettings().joinLocal(3552))
      val tempOutputFile = Files.createTempFile("output", ".conf").toString
      val getMinimalArgs = Array("get", repoPath2, "-o", tempOutputFile)
      cliMain3.start(getMinimalArgs)
      new String(Files.readAllBytes(Paths.get(tempOutputFile))) shouldEqual inputFileContents

    }

    runOn(client2) {
      enterBarrier("server-started")
      val actorRuntime = new ActorRuntime(ActorSystem())
      import actorRuntime._
      val configService = ConfigClientFactory.adminApi(actorSystem, locationService)

      enterBarrier("member1-create")
      val actualConfigValue = configService.getLatest(Paths.get(repoPath1)).await.get.toStringF.await
      actualConfigValue shouldBe inputFileContents

      enterBarrier("member1-update")
      val actualUpdatedConfigValue = configService.getLatest(Paths.get(repoPath1)).await.get.toStringF.await
      actualUpdatedConfigValue shouldBe updatedInputFileContents

      enterBarrier("member1-setActive")
      val actualActiveConfigValue = configService.getActive(Paths.get(repoPath1)).await.get.toStringF.await
      actualActiveConfigValue shouldBe inputFileContents

      configService.create(Paths.get(repoPath2), ConfigData.fromString(inputFileContents)).await
      enterBarrier("member2-create")
    }
    enterBarrier("after-1")
  }
}
