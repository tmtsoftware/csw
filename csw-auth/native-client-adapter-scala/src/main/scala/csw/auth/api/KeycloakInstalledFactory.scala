package csw.auth.api

import java.io.InputStream
import java.util.Locale

import csw.auth.{InMemoryAuthStore, KeycloakInstalledImpl}
import org.keycloak.adapters.KeycloakDeployment

//todo:add documentation
object KeycloakInstalledFactory {
  def createInstance(
      secretStorage: AuthStorage = InMemoryAuthStore): KeycloakInstalled = {
    new KeycloakInstalledImpl(secretStorage, Locale.ENGLISH)
  }

  def createInstanceWithKeycloakDeployment(
      keycloakDeployment: KeycloakDeployment,
      secretStorage: AuthStorage = InMemoryAuthStore): KeycloakInstalled = {
    new KeycloakInstalledImpl(keycloakDeployment, secretStorage, Locale.ENGLISH)
  }

  def createInstanceWithConfig(
      config: InputStream,
      secretStorage: AuthStorage = InMemoryAuthStore): KeycloakInstalled = {
    new KeycloakInstalledImpl(config, secretStorage, Locale.ENGLISH)
  }
}
