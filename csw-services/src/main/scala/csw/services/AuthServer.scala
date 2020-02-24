package csw.services

import csw.aas.core.deployment.AuthServiceLocation
import csw.location.api.scaladsl.LocationService
import csw.services.internal.Settings
import org.tmt.embedded_keycloak.KeycloakData.{ApplicationUser, Client, ClientRole, Realm}
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class AuthServer(locationService: LocationService, settings: Settings)(implicit ec: ExecutionContext) {
  import settings._

  private val configAdminRole = "admin"

  private val `csw-config-server` = Client(
    "csw-config-server",
    "bearer-only",
    passwordGrantEnabled = false,
    authorizationEnabled = false,
    clientRoles = Set(configAdminRole)
  )

  private val `csw-config-cli` = Client(
    "csw-config-cli",
    "public",
    passwordGrantEnabled = false,
    authorizationEnabled = false
  )

  private val embeddedKeycloak = new EmbeddedKeycloak(
    KeycloakData(
      realms = Set(
        Realm(
          "TMT",
          clients = Set(`csw-config-server`, `csw-config-cli`),
          users = Set(
            ApplicationUser(
              configAdminUsername,
              configAdminPassword,
              clientRoles = Set(ClientRole(`csw-config-server`.name, configAdminRole))
            ),
            ApplicationUser(
              "config-user",
              "config-user"
            )
          )
        )
      )
    )
  )

  def start(): StopHandle =
    Service.start(
      "Auth/AAS Service",
      Await.result(
        embeddedKeycloak
          .startServer()
          .flatMap(sh => new AuthServiceLocation(locationService).register(keycloakPort.toInt).map(_ => sh)),
        20.minutes
      )
    )

}
