package csw.auth.akka.http.adapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.auth.AccessToken

//todo: these checks need to be more strong. we need to check for exp, aud, etc. consider taking help from keycloak to other jwt-scala
private[auth] object Authentication {
  val authenticator: Authenticator[AccessToken] = {
    case Provided(token) ⇒ AccessToken.verifyAndDecode(token).toOption
    case _               ⇒ None
  }
}
