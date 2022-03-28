/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.files

import java.nio.file.Paths

import csw.config.api.ConfigData
import csw.config.server.ServerWiring
import csw.config.server.commons.TestFileUtils
import csw.config.server.commons.TestFutureExtension.RichFuture
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AnnexFileServiceTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private val wiring = new ServerWiring

  import wiring._

  private val testFileUtils = new TestFileUtils(settings)
  private val annexFileDir  = Paths.get(wiring.settings.`annex-files-dir`).toFile

  import actorRuntime._

  val contents =
    """
      |test {We think, this is some annex text!!!!}
      |test2 {We think, this is some annex text!!!!}
      |test3 {We think, this is some annex text!!!!}
      |""".stripMargin

  private val configData = ConfigData.fromString(contents)

  override protected def afterAll(): Unit =
    testFileUtils.deleteDirectoryRecursively(annexFileDir)

  test("storing annex file") {
    val actualSha   = annexFileService.post(configData).await
    val expectedSha = Sha1.fromConfigData(configData).await

    actualSha shouldBe expectedSha
  }

  test("getting annex file") {
    val actualSha = Sha1.fromConfigData(configData).await
    Sha1.fromConfigData(annexFileService.get(actualSha).await.get).await shouldBe actualSha
  }

}
