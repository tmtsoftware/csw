package csw.auth

import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success}

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
