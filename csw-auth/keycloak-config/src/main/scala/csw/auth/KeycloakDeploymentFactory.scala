package csw.auth
import com.typesafe.config.Config
import org.keycloak.adapters.KeycloakDeployment

object KeycloakDeploymentFactory {
  def createInstance(config: Config): KeycloakDeployment = {
    //todo: here we need to convert this config into a KD
    //more info can be found here: https://www.keycloak.org/docs/4.5/securing_apps/index.html#_multi_tenancy
    ???
  }
}
