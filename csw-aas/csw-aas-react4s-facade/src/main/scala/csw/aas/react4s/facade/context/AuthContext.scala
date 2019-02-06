package csw.aas.react4s.facade.context

import com.github.ahnfelt.react4s.JsContext
import csw.aas.react4s.facade.components.internal.JsAuthComponents
import csw.aas.react4s.facade.context.models.Auth

import scala.scalajs.js

object AuthContext extends JsContext[AuthContext](JsAuthComponents.AuthContext)

@js.native
trait AuthContext extends js.Object {
  val auth: Auth        = js.native
  def login(): Boolean  = js.native // fixme: refer AuthContext.jsx
  def logout(): Boolean = js.native // fixme: refer AuthContext.jsx
}
