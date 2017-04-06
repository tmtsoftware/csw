package csw.services.config.internal

import java.io.{FileNotFoundException, IOException}
import java.nio.file.Paths
import java.util.Date

import csw.services.config.common.TestFutureExtension.RichFuture
import csw.services.config.models.{ConfigBytes, ConfigId, ConfigString}
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {
  private val wiring = new Wiring()

  private val configManager = wiring.configManager
  private val svnAdmin = wiring.svnAdmin

  test("create and get") {
    svnAdmin.initSvnRepo(null)
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("/ in the beginning of file path is ignored if provided") {
    svnAdmin.initSvnRepo(null)
    val configValue = "axisName = tromboneAxis"

    val fileName = "csw.conf"
    val file = Paths.get(s"/$fileName").toFile
    val fileWithoutBackslash = Paths.get(fileName).toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await

    intercept[IOException] {
      configManager.create(
        fileWithoutBackslash, ConfigString(configValue), oversize = false, "hello world"
      ).await
    }

    configManager.get(fileWithoutBackslash).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("create an existing file throws IOException") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await

    intercept[IOException] {
      configManager.create(
        file, ConfigString(configValue), oversize = false, "hello world"
      ).await
    }
  }

  test("update and get") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val assmeblyConfigValue = "assemblyHCDCount = 3"
    configManager.update(file, ConfigString(assmeblyConfigValue), "Updated config to assembly").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe assmeblyConfigValue
  }

  test("update throws FileNotFoundException if a file does not exists") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile

    intercept[FileNotFoundException] {
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

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"
    val file = Paths.get("/a/b/csw.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val configId = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, Some(configId)).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
  }

  test("get version by date") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue


    configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val date = new Date()
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
  }

  test("get initial version if the date is before the creation date") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val date = new Date(0L)
    val file = Paths.get("/a.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue


    configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("get history of a file") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    val configIdCreate = configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString(configValue).str

    val configIdUpdate = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val configIdUpdate2 = configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.history(file).await.map(_.id) should contain allOf(configIdCreate, configIdUpdate, configIdUpdate2)

    configManager.history(file, 2).await.size shouldBe 2
    configManager.history(file, 2).await.map(_.id) should contain allOf (configIdUpdate, configIdUpdate2)
  }

  test("list all config files") {
    svnAdmin.initSvnRepo(null)

    val tromboneConfig = Paths.get("trombone.conf").toFile
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf").toFile

    val tromboneConfigId = configManager.create(
      tromboneConfig, ConfigString("axisName = tromboneAxis"), oversize = false, "hello trombone"
    ).await

    val assemblyConfigId = configManager.create(
      assemblyConfig, ConfigString("assemblyHCDCount = 3"), oversize = false, "hello assembly"
    ).await

    configManager.list().await.map(_.id) should contain allOf(tromboneConfigId, assemblyConfigId)
  }

  test("exists returns false if file does not exist") {
    svnAdmin.initSvnRepo(null)

    val file = Paths.get("/a.conf").toFile

    configManager.exists(file).await shouldBe false
  }

  test("exists returns true if file exist") {
    svnAdmin.initSvnRepo(null)
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("a/test.csw.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await

    configManager.exists(file).await shouldBe true
  }

<<<<<<< HEAD
  test("delete existing file") {
    svnAdmin.initSvnRepo(null)
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("tromboneHCD.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    configManager.delete(file).await
    configManager.get(file).await shouldBe None
  }

  test("delete file which does not exists returns FileNotFoundException") {
    val file = Paths.get("tromboneHCD.conf").toFile
    intercept[FileNotFoundException] {
      configManager.delete(file).await
    }
  }

  test("delete removes all versions of a file") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"
    val file = Paths.get("/a/b/csw.conf").toFile
    configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val configId = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.delete(file).await
    configManager.history(file).await.size shouldBe 0
    configManager.get(file, Some(configId)).await shouldBe None
  }

  test("default config file") {
    svnAdmin.initSvnRepo(null)

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    val configIdCreate = configManager.create(
      file, ConfigString(configValue), oversize = false, "hello world"
    ).await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString(configValue).str

    val configIdUpdate = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val configIdUpdate2 = configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.setDefault(file, Some(configIdUpdate)).await
    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
    configManager.resetDefault(file).await
    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
  }
}
