package csw.services.cs.internal

import java.nio.file.Paths

import csw.services.cs.common.TestFutureExtension.RichFuture
import csw.services.cs.models.ConfigData
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {

  test("create") {
    val settings = new Settings
    new SvnManager(settings).initSvnRepo(null)

    val configManager: SvnConfigManager = new SvnConfigManager(settings)

    configManager.create(Paths.get("/a.txt").toFile, ConfigData("ha ha ha"), oversize = false, "hello world").await
  }

}
