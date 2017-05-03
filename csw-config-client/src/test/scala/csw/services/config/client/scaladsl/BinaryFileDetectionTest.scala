package csw.services.config.client.scaladsl

import java.io.InputStream
import java.nio.file.Paths

import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.files.Sha1
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class BinaryFileDetectionTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val clientLocationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552))
  private val httpService  = serverWiring.httpService

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  private val actorRuntime = new ActorRuntime()
  import actorRuntime._

  val configService: ConfigService = ConfigClientFactory.make(actorSystem, clientLocationService)

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    httpService.registeredLazyBinding.await
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
    httpService.shutdown().await
    clientLocationService.shutdown().await
    super.afterAll()
  }

  implicit class RichInputStream(is: InputStream) {
    def toByteArray: Array[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  test("should be able to store and retrieve binary file in annex dir") {
    val fileName   = "smallBinary.bin"
    val path       = Paths.get(getClass.getClassLoader.getResource(fileName).toURI)
    val configData = ConfigData.fromPath(path)
    val configId =
      configService.create(Paths.get(fileName), configData, annex = false, s"committing file: $fileName").await

    val expectedContent = configService.getById(Paths.get(fileName), configId).await.get.toInputStream.toByteArray
    val diskFile        = getClass.getClassLoader.getResourceAsStream(fileName)
    expectedContent shouldBe diskFile.toByteArray

    val svnConfigData =
      configService
        .getById(Paths.get(s"$fileName${serverWiring.settings.`sha1-suffix`}"), configId)
        .await
        .get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }
}
