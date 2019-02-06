package csw.aas.react4s
import csw.aas.react4s.facade.config.AuthConfig

object Config extends AuthConfig {
  override val realm: String    = "example"
  override val clientId: String = "example-app"
}
