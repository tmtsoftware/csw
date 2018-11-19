package csw.auth.nativeapp.adapter
import csw.auth.Keycloak
import csw.auth.nativeapp.adapter.api.{AuthStore, NativeAuthAdapter}
import csw.auth.nativeapp.adapter.internal.NativeAuthAdapterImpl

object NativeAuthServiceFactory {

  def make(): NativeAuthAdapter = new NativeAuthAdapterImpl(Keycloak.deployment)

  def make(secretStore: AuthStore): NativeAuthAdapter = new NativeAuthAdapterImpl(Keycloak.deployment, authStore = secretStore)
}
