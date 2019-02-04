package csw.aas.react4s.facade.components
import com.github.ahnfelt.react4s.{ElementOrComponent, J, JsComponent, JsComponentConstructor}
import csw.aas.react4s.facade.components.mapper.aas

object CheckLogin {
  def apply(error: ElementOrComponent, children: ElementOrComponent*): JsComponentConstructor =
    RawComponent(J("error", error) +: children: _*)

  private object RawComponent extends JsComponent(aas.CheckLogin)
}
