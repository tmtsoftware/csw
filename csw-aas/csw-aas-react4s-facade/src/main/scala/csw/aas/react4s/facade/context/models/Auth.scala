package csw.aas.react4s.facade.context.models

import scala.scalajs.js

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
