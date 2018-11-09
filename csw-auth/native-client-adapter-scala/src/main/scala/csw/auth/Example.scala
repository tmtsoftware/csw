package csw.auth

import csw.auth.api.KeycloakInstalledFactory

import scala.io.StdIn
import scala.util.{Failure, Success}

object Example extends App {
  val keycloak = KeycloakInstalledFactory.createInstance()

  println("login initiated")
  val loggedIn = keycloak.loginCommandLine()
  if (loggedIn) {
    println("logged in!")
    keycloak.getAccessToken() match {
      case Failure(exception) =>
        System.err.println(exception.getMessage)
        sys.exit(1)
      case Success(value) => {
        println(s"welcome ${value.getName}")
        StdIn.readLine("press RETURN to exit")
      }
    }
  } else sys.exit(1)
}
