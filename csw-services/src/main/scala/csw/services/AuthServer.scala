/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import csw.aas.core.deployment.AuthServiceLocation
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Subsystem
import csw.services.internal.FutureExt.*
import csw.services.internal.{ManagedService, Settings}
import org.tmt.embedded_keycloak.KeycloakData.{ApplicationUser, Client, Realm}
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationDouble

class AuthServer(locationService: LocationService, settings: Settings)(implicit ec: ExecutionContext) {
  private val serviceName = "Auth/AAS Service"
  import settings._

  private val configAdminRole  = "config-admin"
  private val personRole       = "person-role"
  private val exampleAdminRole = "example-admin-role"
  private val oswUserRoles: Set[String] =
    Subsystem.values.map(subsystem => s"${subsystem.name.toLowerCase}-user").toSet + "osw-user"

  private val `csw-config-cli` = Client(
    "tmt-frontend-app",
    "public",
    passwordGrantEnabled = true,
    implicitFlowEnabled = true,
    authorizationEnabled = false
  )

  private val configUser: ApplicationUser = ApplicationUser(
    "config-user1",
    "config-user1",
    firstName = "config",
    lastName = "user1",
    email = "config-user1@tmt.org"
  )

  private val eswUser1: ApplicationUser = ApplicationUser(
    "esw-user1",
    "esw-user1",
    firstName = "esw",
    lastName = "user1",
    email = "esw-user1@tmt.org",
    realmRoles = Set("esw-user", configAdminRole)
  )

  private val oswUser1: ApplicationUser = ApplicationUser(
    username = "osw-user1",
    password = "osw-user1",
    firstName = "osw",
    lastName = "user1",
    email = "osw-user1@tmt.org",
    realmRoles = oswUserRoles
  )

  private val irisUser1: ApplicationUser = ApplicationUser(
    "iris-user1",
    "iris-user1",
    firstName = "iris",
    lastName = "user1",
    email = "iris-user1@tmt.org",
    realmRoles = Set("iris-user")
  )

  private val tcsUser1: ApplicationUser = ApplicationUser(
    "tcs-user1",
    "tcs-user1",
    firstName = "tcs",
    lastName = "user1",
    email = "tcs-user1@tmt.org",
    realmRoles = Set("tcs-user")
  )

  private val wfosUser1: ApplicationUser = ApplicationUser(
    "wfos-user1",
    "wfos-user1",
    firstName = "wfos",
    lastName = "user1",
    email = "wfos-user1@tmt.org",
    realmRoles = Set("wfos-user")
  )

  private val applicationUser: ApplicationUser = ApplicationUser(
    "dummy-user",
    "dummy-user",
    firstName = "dummy",
    lastName = "user",
    email = "dummy-user@tmt.org",
    realmRoles = Set(personRole, exampleAdminRole)
  )

  private val configAdminUser: ApplicationUser = ApplicationUser(
    configAdminUsername,
    configAdminPassword,
    firstName = configAdminUsername,
    lastName = configAdminUsername,
    email = s"$configAdminUsername@tmt.org",
    realmRoles = Set(configAdminRole)
  )

  private val embeddedKeycloak = new EmbeddedKeycloak(
    KeycloakData(
      realms = Set(
        Realm(
          "TMT",
          clients = Set(`csw-config-cli`),
          users = Set(eswUser1, configAdminUser, configUser, applicationUser, oswUser1, irisUser1, tcsUser1, wfosUser1),
          realmRoles = Set(configAdminRole, personRole, exampleAdminRole) ++ oswUserRoles
        )
      )
    )
  )

  def aasService(enable: Boolean): ManagedService[StopHandle, Unit] =
    ManagedService[StopHandle, Unit](serviceName, enable, start, stop)

  private val start: () => StopHandle = () =>
    embeddedKeycloak
      .startServer()
      .flatMap(sh => new AuthServiceLocation(locationService).register(keycloakPort.toInt).map(_ => sh))
      .await(20.minutes)

  private val stop: StopHandle => Unit = _.stop()

}
