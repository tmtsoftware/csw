package csw.services.config.server.files

import java.nio.file.Paths

import csw.services.config.api.models.ConfigData
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class OversizeFileServiceTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val wiring = new ServerWiring

  import wiring._

  private val testFileUtils   = new TestFileUtils(settings)
  private val oversizeFileDir = Paths.get(wiring.settings.`annex-files-dir`).toFile

  import actorRuntime._

  val contents =
    """
      |test {We think, this is some annex text!!!!}
      |test2 {We think, this is some annex text!!!!}
      |test3 {We think, this is some annex text!!!!}
      |""".stripMargin

  private val configData = ConfigData.fromString(contents)

  override protected def afterAll(): Unit =
    testFileUtils.deleteDirectoryRecursively(oversizeFileDir)

  test("storing annex file") {
    val actualSha   = oversizeFileService.post(configData).await
    val expectedSha = Sha1.fromConfigData(configData).await

    actualSha shouldBe expectedSha
  }

  test("getting annex file") {
    val actualSha = Sha1.fromConfigData(configData).await
    Sha1.fromConfigData(oversizeFileService.get(actualSha).await.get).await shouldBe actualSha
  }

}
