package csw.auth.akka.http.adapter
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import csw.auth.AccessToken
object SecurityDirectives {

  def permission(name: String, resource: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(at.hasPermission(name, resource))
    }

  def role(name: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(at.hasRole(name))
    }

  def customPolicy(policy: AccessToken => Boolean): Directive0 = {
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(policy(at))
    }
  }

  def customPolicy(policy: => Boolean): Directive0 = {
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(policy)
    }
  }
}
