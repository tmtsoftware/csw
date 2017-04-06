package csw.services.cs.internal

import java.io.{FileNotFoundException, IOException}
import java.nio.file.Paths

import csw.services.cs.common.TestFutureExtension.RichFuture
import csw.services.cs.models.{ConfigBytes, ConfigData, ConfigString}
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {
  val settings = new Settings
  private val svnAdmin = new SvnAdmin(settings)
  private val largeFileManager = new LargeFileManager(settings)
  val configManager: SvnConfigManager = new SvnConfigManager(settings, largeFileManager)

  test("create and get") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str
  }


  test("create an existing file throws IOException") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await

    intercept[IOException]{
      configManager.create(
        file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
      ).await
    }
  }

  test("update and get") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str

    configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 3").str
  }

  test("update throws FileNotFoundException if a file does not exists") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile

    intercept[FileNotFoundException]{
      configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    }
  }

  test("get return None if a file does not exists") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile

    configManager.get(file).await shouldBe None
  }

}
