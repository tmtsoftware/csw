package csw.testkit

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.auth.adapters.akka.http.SecurityDirectives
import csw.config.server.{ServerWiring, Settings}
import csw.testkit.internal.{MockedAuthentication, TestKitUtils}

import scala.concurrent.ExecutionContextExecutor

/**
 * ConfigTestKit supports starting HTTP Config Server backed by SVN
 * and registering it with location service
 *
 * Example:
 * {{{
 *   private val testKit = ConfigTestKit()
 *
 *   // starting Config Server (starts config server on default ports specified in configuration file)
 *   // it will also register ConfigService with location service
 *   testKit.startConfigServer()
 *
 *   // stopping Config Server
 *   testKit.shutdownConfigServer()
 *
 * }}}
 *
 */
final class ConfigTestKit private (system: ActorSystem, serverConfig: Option[Config], testKitSettings: TestKitSettings)
    extends MockedAuthentication {

  implicit lazy val actorSystem: ActorSystem = system
  private[csw] lazy val configWiring: ServerWiring = (serverConfig, testKitSettings.ConfigPort) match {
    case (Some(_config), _) ⇒
      new ServerWiring {
        override lazy val config: Config                         = _config
        override lazy val actorSystem: ActorSystem               = system
        override lazy val securityDirectives: SecurityDirectives = _securityDirectives
      }
    case (_, serverPort) ⇒
      new ServerWiring {
        override lazy val actorSystem: ActorSystem               = system
        override lazy val securityDirectives: SecurityDirectives = _securityDirectives
        override lazy val settings: Settings = new Settings(config) {
          override val `service-port`: Int = serverPort.getOrElse(super.`service-port`)
        }
      }
  }

  private var configServer: Option[Http.ServerBinding] = None

  implicit lazy val ec: ExecutionContextExecutor = configWiring.actorRuntime.ec
  implicit lazy val mat: Materializer            = configWiring.actorRuntime.mat
  implicit lazy val timeout: Timeout             = testKitSettings.DefaultTimeout

  /**
   * Start HTTP Config server on provided port in constructor or configuration and create clean copy of SVN repo
   *
   * If your test's requires accessing/creating configurations from configuration service, then using this method you can start configuration service.
   * Configuration service can be accessed via [[csw.config.api.scaladsl.ConfigClientService]] or [[csw.config.api.scaladsl.ConfigService]]
   * which can be created via [[csw.config.client.scaladsl.ConfigClientFactory]]
   *
   */
  def startConfigServer(): Unit = {
    val (server, _) = TestKitUtils.await(configWiring.httpService.registeredLazyBinding, timeout)
    configServer = Some(server)
    deleteServerFiles()
    configWiring.svnRepo.initSvnRepo()
  }

  /** useful for deleting entire svn repo when test has finished */
  def deleteServerFiles(): Unit = {
    val annexFileDir = Paths.get(configWiring.settings.`annex-files-dir`).toFile
    TestKitUtils.deleteDirectoryRecursively(annexFileDir)
    TestKitUtils.deleteDirectoryRecursively(configWiring.settings.repositoryFile)
  }

  /** terminate HTTP ConfigServer */
  def terminateServer(): Unit = configServer.foreach(TestKitUtils.terminateHttpServerBinding(_, timeout))

  /**
   * Shutdown HTTP Config server
   *
   * When the test has completed, make sure you shutdown config server.
   */
  def shutdownConfigServer(): Unit = {
    deleteServerFiles()
    TestKitUtils.await(Http(actorSystem).shutdownAllConnectionPools(), timeout)
    terminateServer()
    TestKitUtils.coordShutdown(configWiring.actorRuntime.shutdown, timeout)
  }

}

object ConfigTestKit {

  /**
   * Create a ConfigTestKit
   *
   * When the test has completed you should shutdown the config server
   * with [[ConfigTestKit#shutdownConfigServer]].
   */
  def apply(
      actorSystem: ActorSystem = ActorSystem("config-server"),
      serverConfig: Option[Config] = None,
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): ConfigTestKit = new ConfigTestKit(system = actorSystem, serverConfig = serverConfig, testKitSettings = testKitSettings)

  /**
   * Java API for creating ConfigTestKit
   *
   * @param actorSystem
   * @return handle to ConfigTestKit which can be used to start and stop config server
   */
  def create(actorSystem: ActorSystem): ConfigTestKit = apply(actorSystem = actorSystem)

  /**
   * Java API for creating ConfigTestKit
   *
   * @param serverConfig custom configuration with which to start config server
   * @return handle to ConfigTestKit which can be used to start and stop config server
   */
  def create(serverConfig: Config): ConfigTestKit = apply(serverConfig = Some(serverConfig))

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
    apply(serverConfig = Some(serverConfig), testKitSettings = testKitSettings)

}
