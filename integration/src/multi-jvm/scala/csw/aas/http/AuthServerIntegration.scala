package csw.aas.http

import csw.aas.core.commons.AASConnection
import csw.aas.core.deployment.AuthServiceLocation
import csw.location.api
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration}
import csw.location.helpers.LSNodeSpec
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.BeforeAndAfterEach
import org.tmt.embedded_keycloak.KeycloakData.{ApplicationUser, Client, Realm}
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AuthIntegrationTestMultiJvmNode1 extends AuthIntegrationTest
class AuthIntegrationTestMultiJvmNode2 extends AuthIntegrationTest
class AuthIntegrationTestMultiJvmNode3 extends AuthIntegrationTest

//DEOPSCSW-571: Setup sample gateway for piping all requests from single source
//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class AuthIntegrationTest
    extends LSNodeSpec(config = new MultiNodeTestConfig, mode = "http")
    with BeforeAndAfterEach
    with MultiNodeHTTPLocationService {

  import config._

  val testServerPort = 3001
  val keycloakPort   = 8081

  private val defaultTimeout: FiniteDuration = 10.seconds
  private val serverTimeout: FiniteDuration  = 30.minutes

  test("it should return 401 for unauthenticated request | DEOPSCSW-571, DEOPSCSW-579") {

    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(KeycloakData.empty, Settings(printProcessLogs = false))
      val stopHandle       = Await.result(embeddedKeycloak.startServer(), serverTimeout)
      new AuthServiceLocation(locationService).register(Settings.default.port).await
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService.resolve(AASConnection.value, defaultTimeout).await

      val stopHandle = new TestServer(locationService).start(testServerPort).await
      val registration = HttpRegistration(
        HttpConnection(api.models.ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
        testServerPort,
        ""
      )
      locationService.register(registration).await

      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")

      val testServer =
        locationService
          .resolve(
            HttpConnection(api.models.ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
            defaultTimeout
          )
          .await
          .get

      requests
        .post(testServer.uri.toString, check = false)
        .statusCode shouldBe 401
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  test("it should return 200 for authenticated request - valid authorization | DEOPSCSW-571, DEOPSCSW-579") {
    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(
        KeycloakData(
          realms = Set(
            Realm(
              "TMT",
              clients = Set(
                Client("tmt-frontend-app", "public", passwordGrantEnabled = true, authorizationEnabled = false)),
              users = Set(ApplicationUser("john", "secret", realmRoles = Set("admin"))),
              realmRoles = Set("admin")
            )
          )
        )
      )
      val stopHandle = Await.result(embeddedKeycloak.startServer(), serverTimeout)
      new AuthServiceLocation(locationService).register(Settings.default.port).await
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService.resolve(AASConnection.value, defaultTimeout).await

      val stopHandle = new TestServer(locationService).start(testServerPort).await
      val registration =
        api.models.HttpRegistration(
          HttpConnection(ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
          testServerPort,
          ""
        )
      locationService.register(registration).await

      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")

      val token = BearerToken.fromServer(
        keycloakPort,
        "john",
        "secret",
        "TMT",
        "tmt-frontend-app",
        host = testConductor.getAddressFor(keycloak).await.host.get
      )

      val testServer =
        locationService
          .resolve(
            HttpConnection(api.models.ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
            defaultTimeout
          )
          .await
          .get
      requests
        .post(url = testServer.uri.toString, auth = token, check = false)
        .statusCode shouldBe 200
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  test("it should return 403 for unauthorized request | DEOPSCSW-571, DEOPSCSW-579") {
    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(
        KeycloakData(
          realms = Set(
            Realm(
              "TMT",
              clients = Set(
                Client("tmt-frontend-app", "public", passwordGrantEnabled = true, authorizationEnabled = false)
              ),
              users = Set(ApplicationUser("john", "secret")),
              realmRoles = Set("admin")
            )
          )
        )
      )
      val stopHandle = Await.result(embeddedKeycloak.startServer(), serverTimeout)
      new AuthServiceLocation(locationService).register(Settings.default.port).await
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService.resolve(AASConnection.value, defaultTimeout).await

      val stopHandle = new TestServer(locationService).start(testServerPort).await
      val registration =
        api.models.HttpRegistration(
          HttpConnection(api.models.ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
          testServerPort,
          ""
        )
      locationService.register(registration).await

      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")

      val token = BearerToken.fromServer(
        keycloakPort,
        "john",
        "secret",
        "TMT",
        "tmt-frontend-app",
        host = testConductor.getAddressFor(keycloak).await.host.get
      )

      val testServer =
        locationService
          .resolve(
            HttpConnection(api.models.ComponentId(Prefix(Subsystem.CSW, "TestServer"), ComponentType.Service)),
            defaultTimeout
          )
          .await
          .get
      requests
        .post(url = testServer.uri.toString, auth = token, check = false)
        .statusCode shouldBe 403
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }
}
