package csw.aas.react4s.facade.context.models

import scala.scalajs.js

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
