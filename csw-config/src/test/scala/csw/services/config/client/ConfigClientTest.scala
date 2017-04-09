package csw.services.config.client

import java.nio.file.Paths

import csw.services.config.commons.TestFileUtils
import csw.services.config.commons.TestFutureExtension.RichFuture
import csw.services.config.models.{ConfigBytes, ConfigString}
import csw.services.config.server.Wiring
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConfigClientTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val wiring = new Wiring()
  import wiring._
  private val testFileUtils = new TestFileUtils(settings)

  private val oversizeFileDir = Paths.get(settings.`oversize-files-dir`).toFile
  private val tmpDir = Paths.get(settings.`tmp-dir`).toFile
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
    testFileUtils.deleteDirectoryRecursively(tmpDir)
    testFileUtils.deleteDirectoryRecursively(oversizeFileDir)
    testFileUtils.deleteDirectoryRecursively(settings.repositoryFile)
  }

  test("should able to create a file and retrieve the same") {
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("test.conf").toFile
    configClient.create(file, ConfigString(configValue), oversize = false, "commit test file").await
    Thread.sleep(1000)
    configClient.get(file).await.get.toFutureString.await shouldBe configValue
  }

}
