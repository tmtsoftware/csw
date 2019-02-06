package csw.aas.react4s.facade.components

import com.github.ahnfelt.react4s.{JsComponent, JsComponentConstructor}
import csw.aas.react4s.facade.components.internal.JsAuthComponents

object Login {
  def apply(): JsComponentConstructor = RawComponent()

  private object RawComponent extends JsComponent(JsAuthComponents.Login)
}
