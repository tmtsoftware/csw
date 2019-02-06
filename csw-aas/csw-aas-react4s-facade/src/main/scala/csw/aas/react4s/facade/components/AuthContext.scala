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
  val token: js.Function0[String]
  val tokenParsed: js.Function0[ParsedToken]
  val realmAccess: js.Function0[Roles]
  val resourceAccess: js.Function0[ResourceAccess]
  val loadUserInfo: js.Function0[AuthPromise[UserInfo, Unit]]
  val isAuthenticated: js.Function0[Boolean]
  val hasRealmRole: js.Function1[String, Boolean]
  val hasResourceRole: js.Function2[String, Option[String], Boolean]
}

trait ParsedToken extends js.Object {
  val exp: Double // todo: can this be instant?
  val iat: Double // todo: can this be instant?
  val nonce: String
  val sub: String
  val session_state: String
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

trait UserInfo extends js.Object {
  val sub: js.UndefOr[String]
  val iss: js.UndefOr[String]
  val aud: js.UndefOr[js.Array[String]]
  val given_name: js.UndefOr[String]
  val family_name: js.UndefOr[String]
  val name: js.UndefOr[String]
  val preferred_username: js.UndefOr[String]
  val email: js.UndefOr[String]
}
