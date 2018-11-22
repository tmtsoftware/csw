package csw.config.server.mocks

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.core.token.AccessToken
import csw.config.api.TokenFactory
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

class JMockedAuthentication extends MockedAuthentication

trait MockedAuthentication extends MockitoSugar {
  val authentication: Authentication = mock[Authentication]
  val securityDirectives             = SecurityDirectives(authentication)

  val roleMissingTokenStr = "rolemissing"
  val validTokenStr       = "valid"
  val invalidTokenStr     = "invalid"

  val roleMissingToken: AccessToken = mock[AccessToken]
  val validToken: AccessToken       = mock[AccessToken]
  val invalidToken: AccessToken     = mock[AccessToken]

  private val authenticator: Authenticator[AccessToken] = {
    case Provided(`roleMissingTokenStr`) ⇒ Some(roleMissingToken)
    case Provided(`validTokenStr`)       ⇒ Some(validToken)
    case _                               ⇒ None
  }

  when(roleMissingToken.hasResourceRole("admin")).thenReturn(false)
  when(validToken.hasResourceRole("admin")).thenReturn(true)
  when(authentication.authenticator).thenReturn(authenticator)

  val roleMissingTokenHeader = Authorization(OAuth2BearerToken(roleMissingTokenStr))
  val validTokenHeader       = Authorization(OAuth2BearerToken(validTokenStr))
  val invalidTokenHeader     = Authorization(OAuth2BearerToken(invalidTokenStr))

  val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)

}
