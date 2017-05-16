package csw.services.config.client.scaladsl.demo

import java.nio.file.Paths

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Future

class ConfigClientDemoExample extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552))
  private val httpService  = serverWiring.httpService

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  //#create-api
  //config client API
  val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  //config admin API
  val configAdminService: ConfigService = ConfigClientFactory.adminApi(actorSystem, locationService)
  //#create-api

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeAll(): Unit =
    httpService.registeredLazyBinding.await

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    locationService.shutdown().await
  }

  val configValue1: String =
    """
      |axisName1 = tromboneAxis1
      |axisName2 = tromboneAxis2
      |axisName3 = tromboneAxis3
      |""".stripMargin

  test("exists") {
    //#exists-snip1
    //construct the path
    val file = Paths.get("/tmt/trmobone/assembly/hcd.conf")
    //#exists-snip1

    // create file using admin API first
    configAdminService.create(file, ConfigData.fromString(configValue1), annex = false, "First commit").await

    //#exists-snip2
    //check if file exists with config service
    val exists: Future[Boolean] = configClientService.exists(file)
    exists.await shouldBe true
    //#exists-snip2
  }

  test("getActive") {
    //#getActive-snip1
    // construct the path
    val file = Paths.get("/tmt/trmobone/assembly/hcd.conf")
    //#getActive-snip1

    configAdminService.create(file, ConfigData.fromString(configValue1), annex = false, "First commit").await

    //#getActive-snip2
    // check that getActive call before any setActive call should return the file with id with which it was created
    val activeFile: Future[Option[ConfigData]] = configClientService.getActive(file)
    activeFile.await.get.toStringF.await shouldBe configValue1
    //#getActive-snip2
  }
}
