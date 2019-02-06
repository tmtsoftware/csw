package csw.aas.react4s.facade.components

import com.github.ahnfelt.react4s.JsContext
import csw.aas.react4s.facade.components.mapper.aas

import scala.scalajs.js

object AuthContext extends JsContext[AuthContext](aas.AuthContext)

@js.native
trait AuthContext extends js.Object {
  val auth: Auth        = js.native
  def login(): Boolean  = js.native // fixme: refer AuthContext.jsx
  def logout(): Boolean = js.native // fixme: refer AuthContext.jsx
}

@js.native
trait Auth extends js.Object {
  def logout(): AuthPromise[Unit, Unit]                                = js.native
  def token(): js.UndefOr[String]                                      = js.native
  def tokenParsed(): js.UndefOr[ParsedToken]                           = js.native
  def realmAccess(): Roles                                             = js.native
  def resourceAccess(): ResourceAccess                                 = js.native
  def loadUserProfile(): AuthPromise[UserProfile, Unit]                = js.native
  def isAuthenticated(): Boolean                                       = js.native
  def hasRealmRole(role: String): Boolean                              = js.native
  def hasResourceRole(role: String, resource: Option[String]): Boolean = js.native
}

@js.native
trait ParsedToken extends js.Object {
  val exp: js.UndefOr[Double]           = js.native // todo: can this be instant?
  val iat: js.UndefOr[Double]           = js.native // todo: can this be instant?
  val nonce: js.UndefOr[String]         = js.native
  val sub: js.UndefOr[String]           = js.native
  val session_state: js.UndefOr[String] = js.native
  val realm_access: Roles               = js.native
  val resource_access: ResourceAccess   = js.native
}

@js.native
trait ResourceAccess extends js.Object {
  val values: js.Dictionary[Roles] = js.native
}

@js.native
trait Roles extends js.Object {
  val roles: js.Array[String] = js.native
}

@js.native
trait AuthPromise[Success, Error] extends js.Object {

  type Callback[T] = js.Function1[T, Unit]

  /**
   * Function to call if the promised action succeeds.
   */
  def success(callback: Callback[Success]): AuthPromise[Success, Error] = js.native

  /**
   * Function to call if the promised action throws an error.
   */
  def error(callback: Callback[Error]): AuthPromise[Success, Error] = js.native
}

@js.native
trait UserProfile extends js.Object {
  val id: js.UndefOr[String]               = js.native
  val username: js.UndefOr[String]         = js.native
  val email: js.UndefOr[String]            = js.native
  val firstName: js.UndefOr[String]        = js.native
  val lastName: js.UndefOr[String]         = js.native
  val enabled: js.UndefOr[Boolean]         = js.native
  val emailVerified: js.UndefOr[Boolean]   = js.native
  val totp: js.UndefOr[Boolean]            = js.native
  val createdTimestamp: js.UndefOr[Double] = js.native
}
