package csw.services.config.internal

import java.io._
import java.nio.file.Paths
import java.util.Date

import csw.services.config.common.TestFutureExtension.RichFuture
import csw.services.config.models.{ConfigBytes, ConfigFileHistory, ConfigFileInfo, ConfigString}
import net.codejava.security.HashGeneratorUtils
import org.scalatest.Matchers

class SvnConfigManagerTest extends org.scalatest.FunSuite with Matchers {
  private val wiring = new Wiring()
  private val configManager = wiring.configManager
  private val svnAdmin = wiring.svnAdmin

  test("create and get") {
    svnAdmin.initSvnRepo()
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("/a.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("/ in the beginning of file path is ignored if provided") {
    svnAdmin.initSvnRepo()
    val configValue = "axisName = tromboneAxis"

    val fileName = "csw.conf"
    val file = Paths.get(s"/$fileName").toFile
    val fileWithoutBackslash = Paths.get(fileName).toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await

    intercept[IOException] {
      configManager.create(fileWithoutBackslash, ConfigString(configValue), oversize = false, "hello world").await
    }

    configManager.get(fileWithoutBackslash).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("creating an existing file throws IOException") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val file = Paths.get("/a.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await

    intercept[IOException] {
      configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    }
  }

  test("update and get") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val file = Paths.get("/a.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val assmeblyConfigValue = "assemblyHCDCount = 3"
    configManager.update(file, ConfigString(assmeblyConfigValue), "Updated config to assembly").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe assmeblyConfigValue
  }

  test("update throws FileNotFoundException if a file does not exists") {
    svnAdmin.initSvnRepo()

    val file = Paths.get("/a.conf").toFile

    intercept[FileNotFoundException] {
      configManager.update(file, ConfigString("assemblyHCDCount = 3"), "Updated config to assembly").await
    }
  }

  test("get return None if a file does not exists") {
    svnAdmin.initSvnRepo()

    val file = Paths.get("/a.conf").toFile

    configManager.get(file).await shouldBe None
  }

  test("get specific version by config id") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"
    val file = Paths.get("/a/b/csw.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val configId = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, Some(configId)).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
  }

  test("get version by date") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val date = new Date()
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
  }

  test("get initial version if the date is before the creation date") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val date = new Date(0L)
    val file = Paths.get("/a.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.get(file, date).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue
  }

  test("get history of a file") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    val configIdCreate = configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString(configValue).str

    val configIdUpdate = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val configIdUpdate2 = configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.history(file).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate, configIdCreate)

    configManager.history(file, 2).await.size shouldBe 2
    configManager.history(file, 2).await.map(_.id) shouldBe List(configIdUpdate2, configIdUpdate)
  }

  test("list all config files") {
    svnAdmin.initSvnRepo()

    val tromboneConfig = Paths.get("trombone.conf").toFile
    val assemblyConfig = Paths.get("a/b/assembly/assembly.conf").toFile

    val tromboneConfigComment = "hello trombone"
    val assemblyConfigComment = "hello assembly"

    val tromboneConfigId = configManager.create(tromboneConfig, ConfigString("axisName = tromboneAxis"), oversize = false, tromboneConfigComment).await
    val assemblyConfigId = configManager.create(assemblyConfig, ConfigString("assemblyHCDCount = 3"), oversize = false, assemblyConfigComment).await

    val tromboneConfigInfo: ConfigFileInfo = ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment)
    val assemblyConfigInfo: ConfigFileInfo = ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment)

    configManager.list().await shouldBe List(assemblyConfigInfo, tromboneConfigInfo)
  }

  test("exists returns false if file does not exist") {
    svnAdmin.initSvnRepo()

    val file = Paths.get("/a.conf").toFile

    configManager.exists(file).await shouldBe false
  }

  test("exists returns true if file exist") {
    svnAdmin.initSvnRepo()
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("a/test.csw.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await

    configManager.exists(file).await shouldBe true
  }

  test("delete existing file") {
    svnAdmin.initSvnRepo()
    val configValue = "axisName = tromboneAxis"

    val file = Paths.get("tromboneHCD.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await

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
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"
    val file = Paths.get("/a/b/csw.conf").toFile
    configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe configValue

    val configId = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.history(file).await.size shouldBe 3
    configManager.delete(file).await
    configManager.history(file).await.size shouldBe 0
    configManager.get(file, Some(configId)).await shouldBe None
  }

  test("default config file") {
    svnAdmin.initSvnRepo()

    val configValue = "axisName = tromboneAxis"
    val assemblyConfigValue = "assemblyHCDCount = 3"
    val newAssemblyConfigValue = "assemblyHCDCount = 5"

    val file = Paths.get("/a.conf").toFile
    val configIdCreate = configManager.create(file, ConfigString(configValue), oversize = false, "hello world").await
    configManager.get(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe ConfigString(configValue).str

    val configIdUpdate = configManager.update(file, ConfigString(assemblyConfigValue), "Updated config to assembly").await
    val configIdUpdate2 = configManager.update(file, ConfigString(newAssemblyConfigValue), "Updated config to assembly").await

    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
    configManager.setDefault(file, Some(configIdUpdate)).await
    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe assemblyConfigValue
    configManager.resetDefault(file).await
    configManager.getDefault(file).await.get.asInstanceOf[ConfigBytes].toString shouldBe newAssemblyConfigValue
  }

  test("storing and retrieving oversize file") {
    svnAdmin.initSvnRepo()
    val file = Paths.get("SomeOversizeFile.txt").toFile
    val content = "testing oversize file"

    val configId = configManager.create(file, ConfigString(content), true, "committing oversize file").await
    val fileContent = configManager.get(file, Some(configId)).await.get
    fileContent.toString shouldBe content

    val svnConfigData = configManager.get(new File(s"${file.getPath}${wiring.settings.`sha1-suffix`}"), Some(configId)).await.get
    svnConfigData.toString shouldBe HashGeneratorUtils.generateSHA1(content)
  }

  test("listing oversize files"){
    svnAdmin.initSvnRepo()

    val file1 = Paths.get("OversizeFile1.txt").toFile
    val comment1  = "committing oversize file"

    val file2 = Paths.get("OversizeFile2.txt").toFile
    val comment2 = "committing one more oversize file"

    val configId1 = configManager.create(file1, ConfigString("testing oversize file"), true, comment1).await
    val configId2 = configManager.create(file2, ConfigString("testing oversize file"), true, comment2).await

    val fileInfoes: List[ConfigFileInfo] = configManager.list().await

    fileInfoes.toSet shouldBe Set(
      ConfigFileInfo(new File(s"${file1.toPath}${wiring.settings.`sha1-suffix`}"), configId1, comment1),
      ConfigFileInfo(new File(s"${file2.toPath}${wiring.settings.`sha1-suffix`}"), configId2, comment2)
    )
  }

  test("Updating oversize file and retrieving history") {
    svnAdmin.initSvnRepo()
    val file = Paths.get("SomeOversizeFile.txt").toFile
    val creationContent = "testing oversize file"
    val creationComment = "initial commit"
    val creationConfigId = configManager.create(file, ConfigString(creationContent), true, creationComment).await

    val newContent = "testing oversize file, again"
    val newComment = "Updating file"
    val newConfigId = configManager.update(file, ConfigString(newContent), newComment).await

    val creationFileContent = configManager.get(file, Some(creationConfigId)).await.get
    creationFileContent.toString shouldBe creationContent

    val updatedFileContent = configManager.get(file, Some(newConfigId)).await.get
    updatedFileContent.toString shouldBe newContent

    val oldSvnConfigData = configManager.get(new File(s"${file.getPath}${wiring.settings.`sha1-suffix`}"), Some(creationConfigId)).await.get
    oldSvnConfigData.toString shouldBe HashGeneratorUtils.generateSHA1(creationContent)

    val newSvnConfigData = configManager.get(new File(s"${file.getPath}${wiring.settings.`sha1-suffix`}"), Some(newConfigId)).await.get
    newSvnConfigData.toString shouldBe HashGeneratorUtils.generateSHA1(newContent)

    val fileHistories: List[ConfigFileHistory] = configManager.history(file).await

    fileHistories.map(history => (history.id, history.comment)) shouldBe List(
      (newConfigId, newComment),
      (creationConfigId, creationComment)
    )
  }
}
