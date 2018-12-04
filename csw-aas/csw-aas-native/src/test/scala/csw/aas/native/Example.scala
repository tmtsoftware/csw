package csw.aas.native

import java.nio.file.Paths

import csw.aas.core.deployment.AuthConfig
import csw.aas.native.scaladsl.FileAuthStore

import scala.concurrent.duration.DurationLong

object Example extends App {

  val authConfig = AuthConfig.loadFromAppConfig

  val keycloak = NativeAppAuthAdapterFactory.make(authConfig, new FileAuthStore(Paths.get("/tmp/auth")))

  println("login initiated")
  keycloak.login()

  private val expires: Long = keycloak.getAccessToken().get.exp.get
  println(s"Expiring on: $expires")
  println(System.currentTimeMillis() / 1000)

  private val timeLeft: Long = expires - System.currentTimeMillis() / 1000
  println(s"time left to expire: $timeLeft")

  println(keycloak.getAccessTokenString())

  println(keycloak.getAccessTokenString((timeLeft + 100).seconds))
}
