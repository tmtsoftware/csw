package csw.testkit.internal

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.token.AccessToken
import csw.config.api.TokenFactory
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

private[testkit] trait MockedAuthentication extends MockitoSugar {
  private val authentication: Authentication = mock[Authentication]
  val _securityDirectives                    = SecurityDirectives(authentication)

  private val validTokenStr           = "valid"
  private val validToken: AccessToken = mock[AccessToken]

  private val authenticator: Authenticator[AccessToken] = {
    case Provided(`validTokenStr`) ⇒ Some(validToken)
    case _                         ⇒ None
  }

  when(validToken.hasResourceRole("admin")).thenReturn(true)
  when(authentication.authenticator).thenReturn(authenticator)

  private val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)
}
