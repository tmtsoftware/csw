package csw.services.config.server.svn

import java.nio.file.Paths

import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.{ConfigServiceTest, ServerWiring}

class SvnConfigServiceTest extends ConfigServiceTest {
  val serverWiring                          = new ServerWiring()
  override val configService: ConfigService = serverWiring.configService

  override protected def afterAll(): Unit = {
    serverWiring.actorSystem.terminate().await
    super.afterAll()
  }

  // DEOPSCSW-141: Change the 'create' API
  test("create call should create a normal file and active file in repo and reset should not delete active file") {

    val filePath       = Paths.get("/tmt/tcp/redis/text/redis.conf")
    val activeFilePath = Paths.get(filePath.toString + serverWiring.settings.`active-config-suffix`)

    configService.create(filePath, ConfigData.fromString(configValue1), annex = false, "initial commit").await

    configService.exists(filePath).await shouldBe true
    configService.exists(activeFilePath).await shouldBe true

    configService.resetActiveVersion(filePath, "resetting active version to latest").await

    configService.exists(activeFilePath).await shouldBe true
  }

}
