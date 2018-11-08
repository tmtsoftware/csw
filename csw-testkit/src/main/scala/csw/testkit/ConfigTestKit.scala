package csw.testkit

import java.nio.file.Paths

import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.server.ServerWiring
import csw.testkit.internal.TestKitUtils

final class ConfigTestKit private (
    serverConfig: Option[Config] = None,
    val testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
) {

  lazy val configWiring: ServerWiring = (serverConfig, testKitSettings.ConfigPort) match {
    case (Some(config), _) ⇒ ServerWiring.make(config)
    case (_, serverPort)   ⇒ ServerWiring.make(serverPort)
  }
  import configWiring.actorRuntime._

  implicit lazy val timeout: Timeout = testKitSettings.DefaultTimeout

  /**
   * Start HTTP Config server on provided port in constructor or configuration and create clean copy of SVN repo
   *
   * If your test's requires accessing/creating configurations from configuration service, then using this method you can start configuration service.
   * Configuration service can be accessed via [[csw.config.api.scaladsl.ConfigClientService]] or [[csw.config.api.scaladsl.ConfigService]]
   * which can be created via [[csw.config.client.scaladsl.ConfigClientFactory]]
   *
   */
  def startConfigServer(): Unit = {
    TestKitUtils.await(configWiring.httpService.registeredLazyBinding, timeout)
    deleteServerFiles()
    configWiring.svnRepo.initSvnRepo()
  }

  def deleteServerFiles(): Unit = {
    val annexFileDir = Paths.get(configWiring.settings.`annex-files-dir`).toFile
    TestKitUtils.deleteDirectoryRecursively(annexFileDir)
    TestKitUtils.deleteDirectoryRecursively(configWiring.settings.repositoryFile)
  }

  /**
   * Shutdown HTTP Config server
   *
   * When the test has completed, make sure you shutdown config server.
   */
  def shutdownConfigServer(): Unit = {
    deleteServerFiles()
    TestKitUtils.await(Http(configWiring.actorSystem).shutdownAllConnectionPools(), timeout)
    TestKitUtils.coordShutdown(shutdown, timeout)
  }

}

object ConfigTestKit {

  /**
   * Create a ConfigTestKit
   *
   * When the test has completed you should shutdown the config server
   * with [[ConfigTestKit#shutdownConfigServer]].
   *
   */
  def apply(
      serverConfig: Option[Config] = None,
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): ConfigTestKit = new ConfigTestKit(serverConfig, testKitSettings)

  /**
   * Java API for creating ConfigTestKit
   *
   * @param serverConfig custom configuration with which to start config server
   * @return handle to ConfigTestKit which can be used to start and stop config server
   */
  def create(serverConfig: Config): ConfigTestKit = new ConfigTestKit(serverConfig = Some(serverConfig))

  /**
   * Java API for creating ConfigTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to ConfigTestKit which can be used to start and stop config server
   */
  def create(testKitSettings: TestKitSettings): ConfigTestKit =
    apply(testKitSettings = testKitSettings)

  /**
   * Java API for creating ConfigTestKit
   *
   * @param serverConfig custom configuration with which to start config server
   * @param testKitSettings custom testKitSettings
   * @return handle to ConfigTestKit which can be used to start and stop config server
   */
  def create(serverConfig: Config, testKitSettings: TestKitSettings): ConfigTestKit =
    apply(Some(serverConfig), testKitSettings)

}
