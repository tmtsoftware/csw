package csw.aas.react4s.facade.components
import com.github.ahnfelt.react4s.{JsComponent, JsComponentConstructor}
import csw.aas.react4s.facade.components.mapper.aas

object Login {
  def apply(): JsComponentConstructor = RawComponent()

  private object RawComponent extends JsComponent(aas.Login)
}
