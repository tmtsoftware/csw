package csw.location.server.http

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation, HttpRegistration}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.network.utils.{Networks, SocketUtils}
import csw.prefix.models.Prefix
import msocket.impl.HttpError
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.tmt.embedded_keycloak.KeycloakData.{AdminUser, ApplicationUser, Realm}
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

class LocationAuthTestWithKeycloak
    extends HTTPLocationServiceOnPorts(SocketUtils.getFreePort, SocketUtils.getFreePort, auth = true)
    with AnyFunSuiteLike
    with Matchers
    with LocationServiceCodecs {

  private val aasPort: Int = SocketUtils.getFreePort

  private val tokenFactory: () => Option[String] =
    () =>
      Some(
        BearerToken
          .fromServer(port = aasPort, host = Networks().hostname, username = "john", password = "abcd", realm = "test")
          .token
      )

  private var keycloakStopHandle: StopHandle = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    keycloakStopHandle = startKeycloak(aasPort)
    locationWiring.get.locationService.register(HttpRegistration(AASConnection.value, aasPort, "auth")).await
  }

  private implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = locationWiring.get.actorSystem

  test("unregisterAll (protected route) should return 401 when there is no valid token present and keycloak is up and registered") {
    val locationNoAuthClient = HttpLocationServiceFactory.make("localhost", httpPort)
    val httpError            = intercept[HttpError](locationNoAuthClient.unregisterAll().await)
    httpError.statusCode shouldBe 401
  }

  test("register (protected route) should return 200 when when a valid token is present in request") {
    val locationAuthClient = HttpLocationServiceFactory.make("localhost", httpPort, tokenFactory)
    val connection         = HttpConnection(ComponentId(Prefix("TCS.comp1"), ComponentType.Service))
    val servicePort        = 2345
    val registration       = HttpRegistration(connection, servicePort, "abc")
    val registrationResult = locationAuthClient.register(registration).await
    registrationResult.location shouldBe HttpLocation(connection, URI.create(s"http://${Networks().hostname}:$servicePort/abc"))
  }

  private def startKeycloak(port: Int): StopHandle = {
    val keycloakData = KeycloakData(
      AdminUser("admin", "admin"),
      Set(
        Realm(
          name = "test",
          users = Set(ApplicationUser("john", "abcd"))
        )
      )
    )
    val embeddedKeycloak = new EmbeddedKeycloak(keycloakData, Settings(port = port, printProcessLogs = false))
    embeddedKeycloak.startServer().awaitWithTimeout(1.minute)
  }

  override def afterAll(): Unit = {
    keycloakStopHandle.stop()
    super.afterAll()
  }
}
