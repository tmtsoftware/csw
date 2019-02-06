package csw.aas.react4s.facade.context

import com.github.ahnfelt.react4s.{J, JsComponent, JsComponentConstructor, Node}
import csw.aas.react4s.facade.components.internal.JsAuthComponents
import csw.aas.react4s.facade.config.AuthConfig

object AuthContextProvider {
  def apply(config: AuthConfig, children: Node*): JsComponentConstructor =
    RawComponent(J("config", config) +: children: _*)

  private object RawComponent extends JsComponent(JsAuthComponents.AuthContextProvider)
}
