package csw.auth

import java.nio.file.Paths

import csw.auth.api.KeycloakInstalledFactory
import csw.auth.internal.FileAuthStore

object Example extends App {
  val keycloak = KeycloakInstalledFactory.make(new FileAuthStore(Paths.get("/tmp")))

  println("login initiated")
//  keycloak.login()

  println(keycloak.getAccessToken())

  Thread.sleep(500000)

}
