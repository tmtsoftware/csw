package csw.services.config.internal

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Paths

import csw.services.config.common.TestFutureExtension.RichFuture
import net.codejava.security.HashGeneratorUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class OversizeFileManagerTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterAll {

  private val wiring = new Wiring()
  private val manager = wiring.oversizeFileManager
  private val svnAdmin = wiring.svnAdmin
  private val oversizeFileDir = Paths.get(wiring.settings.`oversize-files-dir`).toFile

  private val tempFile = {
    val tempFile = Paths.get("SomeOversizeFile.txt").toFile
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(s"""test {We think, this is some oversize text!!!!}""")
    bw.close()
    tempFile
  }

  override protected def afterAll(): Unit = {
    tempFile.delete()
    svnAdmin.deleteDirectoryRecursively(oversizeFileDir)
  }

  test("storing oversize file") {
    val actualSha = manager.post(tempFile).await
    val expectedSha = HashGeneratorUtils.generateSHA1(tempFile)

    actualSha shouldBe expectedSha
  }

  test("getting oversize file") {
    val actualSha = HashGeneratorUtils.generateSHA1(tempFile)
    val outFile = java.io.File.createTempFile("out", ".txt")
    manager.get(actualSha, outFile).await

    HashGeneratorUtils.generateSHA1(outFile) shouldBe actualSha
    outFile.delete()
  }

}
