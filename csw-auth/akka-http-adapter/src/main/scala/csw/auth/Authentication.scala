package csw.auth

import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success}

//todo: these checks need to be more strong. we need to check for exp, aud, etc. consider taking help from keycloak to other jwt-scala
private[auth] object Authentication {
  val authenticator: Authenticator[AccessToken] = {
    case p @ Provided(token) => {
      AccessToken.decode(token) match {
        case Failure(_)     => None
        case Success(value) => Some(value)
      }
    }
    case _ => None
  }
}
