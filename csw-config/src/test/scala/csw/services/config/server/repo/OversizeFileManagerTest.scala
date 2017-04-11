package csw.services.config.server.repo

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Paths

import csw.services.config.api.commons.{ShaUtils, TestFileUtils}
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

  private val tempFile = {
    val tempFile = Paths.get("SomeOversizeFile.txt").toFile
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(
      s"""
         |test {We think, this is some oversize text!!!!}
         |test2 {We think, this is some oversize text!!!!}
         |test3 {We think, this is some oversize text!!!!}
         |""".stripMargin)
    bw.close()
    tempFile
  }

  override protected def afterAll(): Unit = {
    tempFile.delete()
    testFileUtils.deleteDirectoryRecursively(oversizeFileDir)
  }

  test("storing oversize file") {
    val actualSha = oversizeFileManager.post(ConfigData.fromFile(tempFile)).await
    val expectedSha =  ShaUtils.generateSHA1(tempFile).await

    actualSha shouldBe expectedSha
  }

  test("getting oversize file") {
    val actualSha = ShaUtils.generateSHA1(tempFile).await
    ShaUtils.generateSHA1(oversizeFileManager.get(actualSha).get.source).await shouldBe actualSha
  }

}
