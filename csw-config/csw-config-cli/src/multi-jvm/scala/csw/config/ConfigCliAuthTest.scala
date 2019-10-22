package csw.config

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import akka.actor.typed.scaladsl.adapter._
import akka.remote.testconductor.RoleName
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import csw.aas.core.deployment.AuthServiceLocation
import csw.commons.ResourceReader
import csw.config.cli.args.Options
import csw.config.cli.wiring.Wiring
import csw.config.client.scaladsl.ConfigClientFactory
import csw.config.server.commons.TestFileUtils
import csw.config.server.{ServerWiring, Settings}
import csw.location.helpers.{LSNodeSpec, NMembersAndSeed}
import csw.location.server.http.MultiNodeHTTPLocationService
import org.scalatest.FunSuiteLike
import org.tmt.embedded_keycloak.KeycloakData._
import org.tmt.embedded_keycloak.utils.Ports
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings => KeycloakSettings}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class MultiNodeTestConfig extends NMembersAndSeed(2) {
  val keycloak: RoleName           = seed
  val Vector(configServer, client) = members
}

class ConfigCliAuthTestMultiJvmNode1 extends ConfigCliAuthTest(0)
class ConfigCliAuthTestMultiJvmNode2 extends ConfigCliAuthTest(0)
class ConfigCliAuthTestMultiJvmNode3 extends ConfigCliAuthTest(0)

// DEOPSCSW-43: Access Configuration service from any CSW component
class ConfigCliAuthTest(ignore: Int)
    extends LSNodeSpec(config = new MultiNodeTestConfig, mode = "http")
    with MultiNodeHTTPLocationService
    with FunSuiteLike {

  import config._
  import system.dispatcher

  private val adminUser     = "config-admin"
  private val adminPassword = "config-admin"

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  private val defaultTimeout: FiniteDuration = 10.seconds
  private val serverTimeout: FiniteDuration  = 30.minutes

  private val keycloakPort = 8081

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()

    //this will ensure that keycloak does not continue to run after a test failure due to any exception
    Ports.stop(keycloakPort)
  }

  test("should upload, update, get and set active version of configuration files") {
    runOn(keycloak) {
      val configAdmin = "admin"

      val `csw-config-server` = Client(
        "csw-config-server",
        "bearer-only",
        passwordGrantEnabled = false,
        authorizationEnabled = true,
        clientRoles = Set(configAdmin)
      )

      val `csw-config-cli` = Client("csw-config-cli", "public", passwordGrantEnabled = false, authorizationEnabled = false)

      val embeddedKeycloak = new EmbeddedKeycloak(
        KeycloakData(
          realms = Set(
            Realm(
              "TMT",
              clients = Set(`csw-config-server`, `csw-config-cli`),
              users = Set(
                ApplicationUser(
                  adminUser,
                  adminPassword,
                  clientRoles = Set(ClientRole(`csw-config-server`.name, configAdmin))
                )
              )
            )
          )
        )
      )
      val stopHandle = Await.result(embeddedKeycloak.startServer(), serverTimeout)
      Await.result(new AuthServiceLocation(locationService).register(KeycloakSettings.default.port), defaultTimeout)
      enterBarrier("keycloak started")
      enterBarrier("config-server-started")
      enterBarrier("test-finished")
      stopHandle.stop()
    }

    runOn(configServer) {
      enterBarrier("keycloak started")
      val serverWiring = ServerWiring.make(ConfigFactory.parseString("auth-config.client-id = csw-config-server"))
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await
      enterBarrier("config-server-started")
      enterBarrier("test-finished")
    }

    runOn(client) {
      implicit val mat: Materializer = Materializer(typedSystem)
      val (filePath, fileContents)   = ResourceReader.readAndCopyToTmp("/tromboneHCDContainer.conf")
      val repoPath1                  = Paths.get("/client1/hcd/text/tromboneHCDContainer.conf")

      enterBarrier("keycloak started")
      enterBarrier("config-server-started")

      val wiring = Wiring.noPrinting(ConfigFactory.parseString("auth-config.client-id = csw-config-cli"))
      val runner = wiring.commandLineRunner

      val stream = new ByteArrayInputStream(s"$adminUser\n$adminPassword".getBytes())

      val stdIn = System.in
      try {
        System.setIn(stream)
        runner.login(Options(console = true))
      } finally {
        System.setIn(stdIn)
      }

      runner.create(
        Options(relativeRepoPath = Some(repoPath1), inputFilePath = Some(filePath), comment = Some("test"))
      )

      val configService     = ConfigClientFactory.clientApi(system.toTyped, locationService)
      val actualConfigValue = configService.getActive(repoPath1).await.get.toStringF.await
      actualConfigValue shouldBe fileContents
      enterBarrier("test-finished")
    }
    enterBarrier("end")
  }
}
