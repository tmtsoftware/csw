package csw.auth.api

import java.io.InputStream

import csw.auth.internal.KeycloakInstalledImpl
import org.keycloak.adapters.KeycloakDeployment

//todo:add documentation
object KeycloakInstalledFactory {
  def make(): KeycloakInstalledApi = new KeycloakInstalledImpl()

  def make(secretStore: AuthStore): KeycloakInstalledApi = new KeycloakInstalledImpl(authStore = secretStore)

  def make(keycloakDeployment: KeycloakDeployment): KeycloakInstalledApi = new KeycloakInstalledImpl(keycloakDeployment)

  def make(keycloakDeployment: KeycloakDeployment, secretStore: AuthStore): KeycloakInstalledApi =
    new KeycloakInstalledImpl(keycloakDeployment, secretStore)

  def make(config: InputStream): KeycloakInstalledApi = new KeycloakInstalledImpl(config)
}
