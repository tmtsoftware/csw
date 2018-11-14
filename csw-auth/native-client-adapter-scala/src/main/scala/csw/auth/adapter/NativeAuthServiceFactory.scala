package csw.auth.adapter
import java.io.InputStream

import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.adapter.internal.NativeAuthServiceImpl
import org.keycloak.adapters.KeycloakDeployment

//todo:add documentation
object NativeAuthServiceFactory {
  def make(): NativeAuthService = new NativeAuthServiceImpl()

  def make(secretStore: AuthStore): NativeAuthService = new NativeAuthServiceImpl(authStore = secretStore)

  def make(keycloakDeployment: KeycloakDeployment): NativeAuthService = new NativeAuthServiceImpl(keycloakDeployment)

  def make(keycloakDeployment: KeycloakDeployment, secretStore: AuthStore): NativeAuthService =
    new NativeAuthServiceImpl(keycloakDeployment, secretStore)

  def make(config: InputStream): NativeAuthService = new NativeAuthServiceImpl(config)
}
