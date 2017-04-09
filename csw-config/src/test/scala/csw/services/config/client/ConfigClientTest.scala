package csw.services.config.client

import java.nio.file.Paths

import csw.services.config.commons.TestFileUtils
import csw.services.config.commons.TestFutureExtension.RichFuture
import csw.services.config.models.ConfigString
import csw.services.config.server.ServerWiring
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConfigClientTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val serverWiring = new ServerWiring()
  import serverWiring.{httpService, settings, svnAdmin}

  private val clientWiring = new ClientWiring()
  import clientWiring._

  private val testFileUtils = new TestFileUtils(settings)

  import actorRuntime._

  override protected def beforeAll(): Unit = {
    httpService.lazyBinding.await
  }

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
  }

  override protected def beforeEach(): Unit = {
    svnAdmin.initSvnRepo()
  }

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
  }

  test("should able to create a file and retrieve the same") {
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("test.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "commit test file").await
    Thread.sleep(1000)
    configManager.get(file).await.get.toFutureString.await shouldBe configValue
  }

}
