package csw.services.cs.internal

import java.nio.file.Paths

import csw.services.cs.common.TestFutureExtension.RichFuture
import csw.services.cs.models.ConfigData
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {

  test("create") {
    val configManager: SvnConfigManager = new SvnConfigManager(new Settings)
    configManager.initSvnRepo(null)

    configManager.create(Paths.get("/a.txt").toFile, ConfigData.apply("ha ha ha"), oversize = false, "hello world").await
  }

}
