package csw.services.config.client.scaladsl

import java.nio.file.Paths

import csw.services.config.api.exceptions.InvalidInput
import csw.services.config.api.models.{ConfigData, FileType}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.files.Sha1
import csw.services.config.server.{ConfigServiceTest, ServerWiring}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory

// DEOPSCSW-138: Split Config API into Admin API and Client API
// DEOPSCSW-80: HTTP based access for configuration file
class ConfigAdminApiTest extends ConfigServiceTest {

  private val clientLocationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552))
  private val httpService  = serverWiring.httpService

  private val actorRuntime = new ActorRuntime()
  import actorRuntime._

  override val configService: ConfigService = ConfigClientFactory.adminApi(actorSystem, clientLocationService)

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

  // DEOPSCSW-47: Unique name for configuration file
  // DEOPSCSW-135: Validation of suffix for active and sha files
  test("should throw exception for invalid path") {
    val filePath = Paths.get("/test/sample.$active")

    intercept[InvalidInput] {
      configService.create(filePath, ConfigData.fromString(configValue1), annex = false, "invalid path").await
    }
  }

  // DEOPSCSW-27: Storing binary component configurations
  // DEOPSCSW-81: Storing large files in the configuration service
  // DEOPSCSW-131: Detect and handle oversize files
  test("should be able to store and retrieve binary file in annex dir") {
    val fileName   = "smallBinary.bin"
    val path       = Paths.get(getClass.getClassLoader.getResource(fileName).toURI)
    val configData = ConfigData.fromPath(path)
    val repoPath   = Paths.get(fileName)
    val configId =
      configService.create(repoPath, configData, annex = false, s"committing file: $fileName").await

    val expectedContent = configService.getById(repoPath, configId).await.get.toInputStream.toByteArray
    val diskFile        = getClass.getClassLoader.getResourceAsStream(fileName)
    expectedContent shouldBe diskFile.toByteArray

    val list = configService.list(Some(FileType.Annex)).await
    list.map(_.path) shouldBe List(repoPath)

    //Note that configService instance from the server-wiring can be used for assert-only calls for sha files
    //This call is invalid from client side
    val svnConfigData =
      serverWiring.configService
        .getById(Paths.get(s"$fileName${serverWiring.settings.`sha1-suffix`}"), configId)
        .await
        .get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  //DEOPSCSW-75 List the names of configuration files that match a path
  test("should throw invalid input exception if pattern is invalid") {
    intercept[InvalidInput] {
      configService.list(pattern = Some("?i)")).await
    }
  }
}
