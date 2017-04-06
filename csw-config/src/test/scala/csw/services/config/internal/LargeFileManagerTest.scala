package csw.services.config.internal

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Paths

import csw.services.config.common.TestFutureExtension.RichFuture
import net.codejava.security.HashGeneratorUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class LargeFileManagerTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterAll {

  private val manager = new LargeFileManager(new Wiring().settings)

  private val tempFile = {
    val tempFile = Paths.get("SomeLargeFile.txt").toFile
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(s"""test {We think, this is some large text!!!!}""")
    bw.close()
    tempFile
  }

  override protected def afterAll(): Unit = {
    tempFile.delete()
  }

  test("storing large file") {
    val actualSha = manager.post(tempFile).await
    val expectedSha = HashGeneratorUtils.generateSHA1(tempFile)

    actualSha shouldBe expectedSha
  }

  test("getting large file") {
    val actualSha = HashGeneratorUtils.generateSHA1(tempFile)
    val outFile = java.io.File.createTempFile("out", ".txt")
    val file = manager.get(actualSha, outFile).await

    HashGeneratorUtils.generateSHA1(outFile) shouldBe actualSha
  }

}
