package csw.services.config.server.repo

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Paths

import csw.services.config.api.commons.TestFileUtils
import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.models.ConfigData
import csw.services.config.server.ServerWiring
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class OversizeFileManagerTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val wiring = new ServerWiring

  import wiring._

  private val testFileUtils = new TestFileUtils(settings)
  private val oversizeFileDir = Paths.get(wiring.settings.`oversize-files-dir`).toFile

  import actorRuntime._

  val contents =
    """
      |test {We think, this is some oversize text!!!!}
      |test2 {We think, this is some oversize text!!!!}
      |test3 {We think, this is some oversize text!!!!}
      |""".stripMargin

  private val configData = ConfigData.fromString(contents)

  override protected def afterAll(): Unit = {
    testFileUtils.deleteDirectoryRecursively(oversizeFileDir)
  }

  test("storing oversize file") {
    val actualSha = oversizeFileManager.post(configData).await
    val expectedSha = Sha1.fromConfigData(configData).await

    actualSha shouldBe expectedSha
  }

  test("getting oversize file") {
    val actualSha = Sha1.fromConfigData(configData).await
    Sha1.fromConfigData(oversizeFileManager.get(actualSha).await.get).await shouldBe actualSha
  }

}
