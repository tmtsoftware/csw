package csw.auth.adapters.akka.http
import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.auth.token.{AccessToken, RPT}

//todo: these checks need to be more strong. we need to check for exp, aud, etc. consider taking help from keycloak to other jwt-scala
private[auth] object Authentication {
  //todo: can we use DI and inject authenticator where it is used, and make this a def
  val authenticator: Authenticator[AccessToken] = {
    case Provided(token) ⇒ RPT.create(token).toOption
    case _               ⇒ None
  }
}
