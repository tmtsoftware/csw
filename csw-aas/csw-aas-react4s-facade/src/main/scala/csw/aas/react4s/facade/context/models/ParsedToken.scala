package csw.aas.react4s.facade.context.models

import scala.scalajs.js

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
