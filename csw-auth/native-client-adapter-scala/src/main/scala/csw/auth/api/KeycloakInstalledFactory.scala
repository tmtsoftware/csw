package csw.auth.api

import java.io.InputStream

import csw.auth.{InMemoryAuthStore, KeycloakInstalledImpl}
import org.keycloak.adapters.KeycloakDeployment

//todo:add documentation
object KeycloakInstalledFactory {
  def createInstance(secretStorage: AuthStorage = InMemoryAuthStore): KeycloakInstalledApi = {
    new KeycloakInstalledImpl()
  }

  def createInstanceWithKeycloakDeployment(keycloakDeployment: KeycloakDeployment): KeycloakInstalledApi = {
    new KeycloakInstalledImpl(keycloakDeployment)
  }

  def createInstanceWithConfig(config: InputStream): KeycloakInstalledApi = {
    new KeycloakInstalledImpl(config)
  }
}
