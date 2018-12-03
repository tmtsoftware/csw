package csw.aas.native

import csw.aas.core.deployment.Keycloak
import csw.aas.native.api.{AuthStore, NativeAppAuthAdapter}
import csw.aas.native.internal.NativeAppAuthAdapterImpl

//todo: can we use DI and inject Keycloak here?
object NativeAppAuthAdapterFactory {

  def make(): NativeAppAuthAdapter = new NativeAppAuthAdapterImpl(Keycloak.deployment)

  def make(secretStore: AuthStore): NativeAppAuthAdapter =
    new NativeAppAuthAdapterImpl(Keycloak.deployment, authStore = secretStore)
}
