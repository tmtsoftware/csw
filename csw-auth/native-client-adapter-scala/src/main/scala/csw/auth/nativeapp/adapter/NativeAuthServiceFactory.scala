package csw.auth.nativeapp.adapter
import csw.auth.KeycloakDeployment
import csw.auth.nativeapp.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.nativeapp.adapter.internal.NativeAuthServiceImpl

object NativeAuthServiceFactory {

  private val kd = KeycloakDeployment.instance

  def make(): NativeAuthService = new NativeAuthServiceImpl(kd)

  def make(secretStore: AuthStore): NativeAuthService = new NativeAuthServiceImpl(kd, authStore = secretStore)
}
