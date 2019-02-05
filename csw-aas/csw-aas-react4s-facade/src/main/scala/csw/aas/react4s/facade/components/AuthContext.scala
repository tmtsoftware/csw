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
  val logout: js.Function0[Unit] // fixme: this actually returns Promise from js side
  val token: js.Function0[String]
  val tokenParsed: js.Function0[ParsedToken]
  val realmAccess: js.Function0[Roles]
  val resourceAccess: js.Function0[ResourceAccess]
  val loadUserInfo: js.Function0[String] // fixme: not supported, refer Auth.jsx
  val isAuthenticated: js.Function0[Boolean]
  def hasRealmRole: js.Function1[String, Boolean]
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
