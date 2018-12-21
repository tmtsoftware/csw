package csw.aas.http

import csw.aas.core.deployment.AuthServiceLocation
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.helpers.LSNodeSpec
import csw.location.server.http.MultiNodeHTTPLocationService
import org.scalatest.BeforeAndAfterEach
import tech.bilal.embedded_keycloak.KeycloakData.{ApplicationUser, Client, Realm}
import tech.bilal.embedded_keycloak.utils.{BearerToken, Ports}
import tech.bilal.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt

class AuthIntegrationTestMultiJvmNode1 extends AuthIntegrationTest
class AuthIntegrationTestMultiJvmNode2 extends AuthIntegrationTest
class AuthIntegrationTestMultiJvmNode3 extends AuthIntegrationTest

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class AuthIntegrationTest
    extends LSNodeSpec(config = new MultiNodeTestConfig, mode = "http")
    with BeforeAndAfterEach
    with MultiNodeHTTPLocationService {

  import config._

  val testServerPort = 3001
  val keycloakPort   = 8081

  test("it should return 401 for unauthenticated request") {

    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(KeycloakData.empty)
      val stopHandle =
        Await.result(embeddedKeycloak.startServerInBackground(), 30.minutes)
      Await.result(
        new AuthServiceLocation(locationService)
          .register(Settings.default.port),
        10.seconds
      )
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService
        .resolve(
          HttpConnection(ComponentId("AAS", ComponentType.Service)),
          10.seconds
        )
        .await
      val stopHandle = Await.result(new TestServer(locationService).start(testServerPort), 10.seconds)
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      requests
        .post(s"http://localhost:$testServerPort")
        .statusCode shouldBe 401
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  test("it should return 200 for authenticated request - valid authorization") {
    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(
        KeycloakData(
          realms = Set(
            Realm(
              "example",
              clients = Set(
                Client("my-app", "public", passwordGrantEnabled = true, authorizationEnabled = true),
                Client("my-server", "bearer-only", passwordGrantEnabled = false, authorizationEnabled = true),
              ),
              users = Set(ApplicationUser("john", "secret", realmRoles = Set("admin"))),
              realmRoles = Set("admin")
            )
          )
        )
      )

      val stopHandle =
        Await.result(embeddedKeycloak.startServerInBackground(), 30.minutes)
      Await.result(
        new AuthServiceLocation(locationService)
          .register(Settings.default.port),
        10.seconds
      )
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService
        .resolve(
          HttpConnection(ComponentId("AAS", ComponentType.Service)),
          10.seconds
        )
        .await
      val stopHandle = Await.result(new TestServer(locationService).start(testServerPort), 10.seconds)
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")

      val token = BearerToken.getBearerToken(keycloakPort,
                                             "john",
                                             "secret",
                                             "example",
                                             "my-app",
                                             host = Await.result(testConductor.getAddressFor(keycloak), 10.seconds).host.get)

      requests
        .post(url = s"http://localhost:$testServerPort", auth = token)
        .statusCode shouldBe 200
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  test("it should return 403 for unauthorized request") {
    runOn(keycloak) {
      val embeddedKeycloak = new EmbeddedKeycloak(
        KeycloakData(
          realms = Set(
            Realm(
              "example",
              clients = Set(
                Client("my-app", "public", passwordGrantEnabled = true, authorizationEnabled = true)
              ),
              users = Set(ApplicationUser("john", "secret")),
              realmRoles = Set("admin")
            )
          )
        )
      )

      val stopHandle =
        Await.result(embeddedKeycloak.startServerInBackground(), 30.minutes)
      Await.result(
        new AuthServiceLocation(locationService)
          .register(Settings.default.port),
        10.seconds
      )
      enterBarrier("keycloak started")
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.stop()
    }

    runOn(exampleServer) {
      enterBarrier("keycloak started")
      locationService
        .resolve(
          HttpConnection(ComponentId("AAS", ComponentType.Service)),
          10.seconds
        )
        .await
      val stopHandle = Await.result(new TestServer(locationService).start(testServerPort), 10.seconds)
      enterBarrier("test-server started")
      enterBarrier("test finished")
      stopHandle.unbind()
    }

    runOn(testClient) {
      enterBarrier("keycloak started")
      enterBarrier("test-server started")

      val token = BearerToken.getBearerToken(keycloakPort,
                                             "john",
                                             "secret",
                                             "example",
                                             "my-app",
                                             host = Await.result(testConductor.getAddressFor(keycloak), 10.seconds).host.get)

      requests
        .post(url = s"http://localhost:$testServerPort", auth = token)
        .statusCode shouldBe 403
      enterBarrier("test finished")
    }

    enterBarrier("end")
  }

  override def beforeEach(): Unit = {
    new Ports().stop(keycloakPort)
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    new Ports().stop(keycloakPort)
  }
}
