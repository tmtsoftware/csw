package csw.services.cs.internal

import java.io.{FileNotFoundException, IOException}
import java.nio.file.Paths
import java.util.Date

import csw.services.cs.common.TestFutureExtension.RichFuture
import csw.services.cs.models.{ConfigBytes, ConfigData, ConfigString}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {
  val settings = new Settings
  private val svnAdmin = new SvnAdmin(settings)
  private val largeFileManager = new LargeFileManager(settings)
  val configManager: SvnConfigManager = new SvnConfigManager(settings, largeFileManager)

  implicit val ec = ExecutionContext.Implicits.global
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

  test("get specific version by config id") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str

    val configId = configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    configManager.update(file, ConfigString("assemblyHCDCount = 5"), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 5").str
    configManager.get(file, Some(configId)).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 3").str
  }

  test("get version by date") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str


    configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    val date = new Date()
    configManager.update(file, ConfigString("assemblyHCDCount = 5"), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 5").str
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 3").str
  }

  test("get initial version if the date is before the creation date") {
    svnAdmin.initSvnRepo(null)

    val date = new Date(0L)
    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str


    configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    configManager.update(file, ConfigString("assemblyHCDCount = 5"), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("assemblyHCDCount = 5").str
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str
  }

  test("get history of all versions of a file") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString("axisName = tromboneAxis"), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString("axisName = tromboneAxis").str

    configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    configManager.update(file, ConfigString("assemblyHCDCount = 5"), "Updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
  }

}
