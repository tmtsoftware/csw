package csw.services.config.client.scaladsl.demo

import java.io.InputStream
import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.services.config.api.models.{ConfigData, ConfigId, FileType}
import csw.services.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest._

import scala.async.Async._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

class ConfigClientDemoExample extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552))
  private val httpService  = serverWiring.httpService

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  //#create-api
  //config client API
  val clientApi: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  //config admin API
  val adminApi: ConfigService = ConfigClientFactory.adminApi(actorSystem, locationService)
  //#create-api

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeAll(): Unit =
    httpService.registeredLazyBinding.await

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    locationService.shutdown().await
  }

  //#declare_string_config
  val defaultStrConf: String =
    """
      |axisName1 = tromboneAxis1
      |axisName2 = tromboneAxis2
      |axisName3 = tromboneAxis3
      |""".stripMargin
  //#declare_string_config

  test("exists") {
    //#exists-snip1
    //construct the path
    val filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf")
    //#exists-snip1

    // create file using admin API first
    adminApi.create(filePath, ConfigData.fromString(defaultStrConf), annex = false, "First commit").await

    val doneF =
      //#exists-snip2
      async {
        //check if file exists with config service
        val exists: Boolean = await(clientApi.exists(filePath))
        exists shouldBe true
      }
    //#exists-snip2
    Await.result(doneF, 5.seconds)
  }

  test("getActive") {
    val doneF =
      //#getActive-snip1
      async {
        // construct the path
        val filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf")
        //#getActive-snip1

        await(adminApi.create(filePath, ConfigData.fromString(defaultStrConf), annex = false, "First commit"))

        //#getActive-snip2
        val activeFile: Option[ConfigData] = await(clientApi.getActive(filePath))
        await(activeFile.get.toStringF) shouldBe defaultStrConf
      }
    //#getActive-snip2
    Await.result(doneF, 5.seconds)
  }

  test("create-update-delete") {
    val futC =
      //#create
      async {
        //construct ConfigData from String containing ASCII text
        val configString: String =
          """
        // Name: ComponentType ConnectionType
        {
          name: lgsTromboneHCD
          type: Hcd
          connectionType: [akka]
        }
        """.stripMargin
        val config1: ConfigData = ConfigData.fromString(configString)

        //construct ConfigData from a local file containing binary data
        val srcFilePath         = getClass.getClassLoader.getResource("smallBinary.bin").toURI
        val config2: ConfigData = ConfigData.fromPath(Paths.get(srcFilePath))

        //construct ConfigData from Array[Byte] by reading a local file
        val stream: InputStream = getClass.getClassLoader.getResourceAsStream("smallBinary.bin")

        def byteArray: Array[Byte] = Stream.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray

        val config3 = ConfigData.fromBytes(byteArray)

        //store the config, at a specified path as normal text file
        val id1: ConfigId =
          await(adminApi.create(Paths.get("/hcd/trombone/overnight.conf"), config1, false, "review done"))

        //store the config, at a specified path as a binary file in annex store
        val id2: ConfigId =
          await(adminApi.create(Paths.get("/hcd/trombone/firmware.bin"), config2, true, "smoke test done"))

        //store the config, at a specified path as a binary file in annex store
        val id3: ConfigId =
          await(adminApi.create(Paths.get("/hcd/trombone/debug.bin"), config3, true, "new file from vendor"))

        //CAUTION: for demo example setup these IDs are returned. Don't assume them in production setup.
        id1 shouldEqual ConfigId(1)
        id2 shouldEqual ConfigId(3)
        id3 shouldEqual ConfigId(5)
      }
    Await.result(futC, 5.seconds)
    //#create

    //#update
    val futU = async {
      val destPath = Paths.get("/hcd/trombone/debug.bin")
      val newId = adminApi
        .update(destPath, ConfigData.fromString(defaultStrConf), comment = "debug statements")
        .await

      //validate the returned id
      newId shouldEqual ConfigId(7)
    }
    Await.result(futU, 5.seconds)
    //#update

    //#delete
    val futD = async {
      val unwantedFilePath = Paths.get("/hcd/trombone/debug.bin")
      adminApi.delete(unwantedFilePath, "no longer needed").await
    }
    Await.result(futD, 5.seconds)
    //#delete
  }

  test("getById") {
    //#getById
    val doneF = async {
      // create a file using API first
      val filePath = Paths.get("/tmt/trmobone/assembly/hcd.conf")
      val id: ConfigId =
        await(adminApi.create(filePath, ConfigData.fromString(defaultStrConf), annex = false, "First commit"))

      //validate
      val actualData = await(adminApi.getById(filePath, id)).get
      await(actualData.toStringF) shouldBe defaultStrConf
    }
    Await.result(doneF, 5.seconds)
    //#getById
  }

  test("getLatest") {
    //#getLatest
    val assertionF: Future[Assertion] = async {
      //create a file
      val filePath = Paths.get("/test.conf")
      await(adminApi.create(filePath, ConfigData.fromString(defaultStrConf), annex = false, "initial configuration"))

      //override the contents
      val newContent = "I changed the contents!!!"
      await(adminApi.update(filePath, ConfigData.fromString(newContent), "changed!!"))

      //get the latest file
      val newConfigData = await(adminApi.getLatest(filePath)).get
      //validate
      await(newConfigData.toStringF) shouldBe newContent
    }
    Await.result(assertionF, 5.seconds)
    //#getLatest
  }

  test("getByTime") {
    //#getByTime
    val assertionF = async {
      val tInitial = Instant.MIN
      //create a file
      val filePath = Paths.get("/a/b/c/test.conf")
      await(adminApi.create(filePath, ConfigData.fromString(defaultStrConf), annex = false, "initial configuration"))

      //override the contents
      val newContent = "I changed the contents!!!"
      await(adminApi.update(filePath, ConfigData.fromString(newContent), "changed!!"))

      val initialData: ConfigData = await(adminApi.getByTime(filePath, tInitial)).get
      await(initialData.toStringF) shouldBe defaultStrConf

      val latestData = await(adminApi.getByTime(filePath, Instant.now())).get
      await(latestData.toStringF) shouldBe newContent
    }
    Await.result(assertionF, 5.seconds)
    //#getByTime
  }

  test("list") {
    //#list
    //Here's a list of tuples containing FilePath and FileTyepe(Annex / Normal)
    val paths: List[(Path, FileType)] = List[(Path, FileType)](
      (Paths.get("a/c/trombone.conf"), FileType.Annex),
      (Paths.get("a/b/c/hcd/hcd.conf"), FileType.Normal),
      (Paths.get("a/b/assembly/assembly1.fits"), FileType.Annex),
      (Paths.get("a/b/c/assembly/assembly2.fits"), FileType.Normal),
      (Paths.get("testing/test.conf"), FileType.Normal)
    )

    //create config files at those paths
    paths map {
      case (path, fileType) ⇒
        val createF = async {
          await(adminApi.create(path, ConfigData.fromString(defaultStrConf), FileType.Annex == fileType,
              "initial commit"))
        }
        Await.result(createF, 2.seconds)
    }

    val assertionF = async {
      //retrieve list of all files
      await(adminApi.list()).map(info ⇒ info.path).toSet shouldBe paths.map {
        case (path, fileType) ⇒ path
      }.toSet
      //retrieve list of files based on type
      await(adminApi.list(Some(FileType.Annex))).size shouldBe 2
      await(adminApi.list(Some(FileType.Normal))).size shouldBe 3
      //retrieve list using pattern
      await(adminApi.list(None, Some(".*.conf"))).size shouldBe 3
      await(adminApi.list(Some(FileType.Normal), Some(".*.conf"))).size shouldBe 2
      await(adminApi.list(Some(FileType.Annex), Some("a/c.*"))).size shouldBe 1
      await(adminApi.list(Some(FileType.Normal), Some("test.*"))).size shouldBe 1
    }
    Await.result(assertionF, 5.seconds)
    //#list
  }
}
