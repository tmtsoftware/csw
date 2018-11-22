package csw.auth.adapters.nativeapp

import csw.auth.adapters.nativeapp.api.{AuthStore, NativeAppAuthAdapter}
import csw.auth.adapters.nativeapp.internal.NativeAppAuthAdapterImpl
import csw.auth.core.Keycloak

//todo: can we use DI and inject Keycloak here?
object NativeAppAuthAdapterFactory {

  def make(): NativeAppAuthAdapter = new NativeAppAuthAdapterImpl(Keycloak.deployment)

  def make(secretStore: AuthStore): NativeAppAuthAdapter =
    new NativeAppAuthAdapterImpl(Keycloak.deployment, authStore = secretStore)
}
