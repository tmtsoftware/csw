package csw.auth.nativeapp.adapter
import csw.auth.Keycloak
import csw.auth.nativeapp.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.nativeapp.adapter.internal.NativeAuthServiceImpl

object NativeAuthServiceFactory {

  def make(): NativeAuthService = new NativeAuthServiceImpl(Keycloak.deployment)

  def make(secretStore: AuthStore): NativeAuthService = new NativeAuthServiceImpl(Keycloak.deployment, authStore = secretStore)
}
