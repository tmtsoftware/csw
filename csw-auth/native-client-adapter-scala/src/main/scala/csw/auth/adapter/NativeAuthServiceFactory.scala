package csw.auth.adapter
import csw.auth.KeycloakDeploymentFactory
import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.adapter.internal.NativeAuthServiceImpl

//todo:add documentation
object NativeAuthServiceFactory {

  val kd = KeycloakDeploymentFactory.createInstance()

  def make(): NativeAuthService = new NativeAuthServiceImpl(kd)

  def make(secretStore: AuthStore): NativeAuthService = new NativeAuthServiceImpl(kd, authStore = secretStore)
}
