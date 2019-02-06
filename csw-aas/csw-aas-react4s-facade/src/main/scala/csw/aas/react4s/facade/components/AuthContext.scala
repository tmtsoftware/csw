package csw.aas.react4s.facade.components

import com.github.ahnfelt.react4s.JsContext
import csw.aas.react4s.facade.components.mapper.aas

import scala.scalajs.js

trait AuthContext extends js.Object {
  val auth: Auth
  val login: js.Function0[Boolean]  // fixme: refer AuthContext.jsx
  val logout: js.Function0[Boolean] // fixme: refer AuthContext.jsx
}

object AuthContext extends JsContext[AuthContext](aas.AuthContext)

trait Auth extends js.Object {
  val logout: js.Function0[AuthPromise[Unit, Unit]]
  val token: js.Function0[js.UndefOr[String]]
  val tokenParsed: js.Function0[js.UndefOr[ParsedToken]]
  val realmAccess: js.Function0[Roles]
  val resourceAccess: js.Function0[ResourceAccess]
  val loadUserProfile: js.Function0[AuthPromise[UserProfile, Unit]]
  val isAuthenticated: js.Function0[Boolean]
  val hasRealmRole: js.Function1[String, Boolean]
  val hasResourceRole: js.Function2[String, Option[String], Boolean]
}

trait ParsedToken extends js.Object {
  val exp: js.UndefOr[Double] // todo: can this be instant?
  val iat: js.UndefOr[Double] // todo: can this be instant?
  val nonce: js.UndefOr[String]
  val sub: js.UndefOr[String]
  val session_state: js.UndefOr[String]
  val realm_access: Roles
  val resource_access: ResourceAccess
}

trait ResourceAccess extends js.Object {
  val values: js.Dictionary[Roles]
}

trait Roles extends js.Object {
  val roles: js.Array[String]
}

trait AuthPromise[Success, Error] extends js.Object {

  type Callback[T] = js.Function1[T, Unit]

  /**
   * Function to call if the promised action succeeds.
   */
  val success: js.Function1[Callback[Success], AuthPromise[Success, Error]]

  /**
   * Function to call if the promised action throws an error.
   */
  val error: js.Function1[Callback[Error], AuthPromise[Success, Error]]
}

trait UserProfile extends js.Object {
  val id: js.UndefOr[String]
  val username: js.UndefOr[String]
  val email: js.UndefOr[String]
  val firstName: js.UndefOr[String]
  val lastName: js.UndefOr[String]
  val enabled: js.UndefOr[Boolean]
  val emailVerified: js.UndefOr[Boolean]
  val totp: js.UndefOr[Boolean]
  val createdTimestamp: js.UndefOr[Double]
}
