package csw.aas.native

import csw.aas.core.deployment.AuthConfig
import csw.aas.native.api.{AuthStore, NativeAppAuthAdapter}
import csw.aas.native.internal.NativeAppAuthAdapterImpl

//todo: can we use DI and inject Keycloak here?
object NativeAppAuthAdapterFactory {

  def make(authConfig: AuthConfig): NativeAppAuthAdapter = new NativeAppAuthAdapterImpl(authConfig)

  def make(authConfig: AuthConfig, secretStore: AuthStore): NativeAppAuthAdapter =
    new NativeAppAuthAdapterImpl(authConfig, authStore = secretStore)
}
