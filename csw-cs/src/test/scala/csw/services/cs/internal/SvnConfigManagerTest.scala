package csw.services.cs.internal

import java.nio.file.Paths

import csw.services.cs.common.TestFutureExtension.RichFuture
import csw.services.cs.models.{ConfigBytes, ConfigData, ConfigString}
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {
  val settings = new Settings
  private val svnAdmin = new SvnAdmin(settings)
  svnAdmin.initSvnRepo(null)
  val configManager: SvnConfigManager = new SvnConfigManager(settings)

  test("create") {
    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str
  }

}
