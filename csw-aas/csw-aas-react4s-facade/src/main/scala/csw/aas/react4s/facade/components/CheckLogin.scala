package csw.aas.react4s.facade.components

import com.github.ahnfelt.react4s._
import csw.aas.react4s.facade.components.internal.JsAuthComponents

object CheckLogin {
  def apply(error: Node, children: Node*): JsComponentConstructor =
    RawComponent(J("error", error) +: children: _*)

  private object RawComponent extends JsComponent(JsAuthComponents.CheckLogin)
}
