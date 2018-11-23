package csw.auth.adapters.akka.http
import csw.auth.core.token.AccessToken

case class User(
    givenName: Option[String] = None,
    familyName: Option[String] = None,
    name: Option[String] = None,
    preferredUsername: Option[String] = None,
    email: Option[String] = None
)

object User {
  private[auth] def apply(accessToken: AccessToken): Option[User] =
    if (accessToken.clientAddress.isEmpty && accessToken.clientHost.isEmpty && accessToken.clientAddress.isEmpty)
      Some(
        User(accessToken.given_name, accessToken.family_name, accessToken.name, accessToken.preferred_username, accessToken.email)
      )
    else None
}
