package csw.location.server.http

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation, HttpRegistration, Metadata}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import msocket.http.HttpError
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.tmt.embedded_keycloak.KeycloakData._
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

class LocationAuthTestWithKeycloak
    extends HTTPLocationServiceOnPorts(SocketUtils.getFreePort, SocketUtils.getFreePort, auth = true)
    with AnyFunSuiteLike
    with Matchers
    with LocationServiceCodecs
    with ScalaFutures {

  private val aasPort: Int = SocketUtils.getFreePort

  private lazy val hostname: String = locationWiring.get.clusterSettings.hostname

  private lazy val tokenFactoryWithAdminRole: () => Option[String]    = getToken("admin", "password1")
  private lazy val tokenFactoryWithoutAdminRole: () => Option[String] = getToken("nonAdmin", "password2")
  private var keycloakStopHandle: StopHandle                          = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    keycloakStopHandle = startKeycloak(aasPort)
    locationWiring.get.locationService.register(HttpRegistration(AASConnection.value, aasPort, "auth")).futureValue
  }

  private implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = locationWiring.get.actorSystem
  private implicit val patience: PatienceConfig                        = PatienceConfig(5.seconds, 100.millis)

  test("list (un-protected route) should return 200 when client does not provide token") {
    val locationClientWithoutToken = HttpLocationServiceFactory.make("localhost", httpPort)
    locationClientWithoutToken.list.futureValue should not be empty
  }

  test(
    "unregisterAll (protected route) should return 401 when client does not provide token | CSW-98, CSW-89"
  ) {
    val locationClientWithoutToken = HttpLocationServiceFactory.make("localhost", httpPort)
    val exception                  = intercept[Exception](locationClientWithoutToken.unregisterAll().futureValue)
    exception.getCause shouldBe a[HttpError]
    exception.getCause.asInstanceOf[HttpError].statusCode shouldBe 401
  }

  test(
    "unregisterAll (protected route) should return 403 when client does not have location admin role in token | CSW-98, " +
      "CSW-89, CSW-106"
  ) {
    val locationClient = HttpLocationServiceFactory.make("localhost", httpPort, tokenFactoryWithoutAdminRole)
    val exception      = intercept[Exception](locationClient.unregisterAll().futureValue)
    exception.getCause shouldBe a[HttpError]
    exception.getCause.asInstanceOf[HttpError].statusCode shouldBe 403
  }

  test("register (protected route) should return 200 when client have location admin role in token | CSW-98, CSW-89, CSW-106") {
    val locationClient     = HttpLocationServiceFactory.make("localhost", httpPort, tokenFactoryWithAdminRole)
    val connection         = HttpConnection(ComponentId(Prefix("TCS.comp1"), ComponentType.Service))
    val servicePort        = 2345
    val registration       = HttpRegistration(connection, servicePort, "abc")
    val registrationResult = locationClient.register(registration).futureValue
    registrationResult.location shouldBe HttpLocation(
      connection,
      URI.create(s"http://$hostname:$servicePort/abc"),
      Metadata().withCSWVersion()
    )
  }

  private def startKeycloak(port: Int): StopHandle = {
    val AdminRole = "location-admin"
    val locationServerClient =
      Client(name = "tmt-frontend-app", clientType = "public", passwordGrantEnabled = true)
    val keycloakData = KeycloakData(
      realms = Set(
        Realm(
          name = "TMT",
          users = Set(
            ApplicationUser("admin", "password1", realmRoles = Set(AdminRole)),
            ApplicationUser("nonAdmin", "password2")
          ),
          clients = Set(locationServerClient),
          realmRoles = Set(AdminRole)
        )
      )
    )
    val embeddedKeycloak = new EmbeddedKeycloak(keycloakData, Settings(port = port, printProcessLogs = false))
    Await.result(embeddedKeycloak.startServer(), 1.minute)
  }

  private def getToken(userName: String, password: String): () => Some[String] = { () =>
    Some(
      BearerToken
        .fromServer(
          realm = "TMT",
          client = "tmt-frontend-app",
          host = hostname,
          port = aasPort,
          username = userName,
          password = password
        )
        .token
    )
  }

  override def afterAll(): Unit = {
    keycloakStopHandle.stop()
    super.afterAll()
  }
}
