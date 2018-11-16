package csw.auth.adapter
import csw.auth.KeycloakDeployment
import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.adapter.internal.NativeAuthServiceImpl

object NativeAuthServiceFactory {

  private val kd = KeycloakDeployment.instance

  def make(): NativeAuthService = new NativeAuthServiceImpl(kd)

  def make(secretStore: AuthStore): NativeAuthService = new NativeAuthServiceImpl(kd, authStore = secretStore)
}
