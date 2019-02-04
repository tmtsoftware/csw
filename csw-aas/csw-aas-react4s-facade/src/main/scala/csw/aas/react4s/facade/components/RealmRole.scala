package csw.aas.react4s.facade.components
import com.github.ahnfelt.react4s.{ElementOrComponent, J, JsComponent, JsComponentConstructor}
import csw.aas.react4s.facade.components.mapper.aas

object RealmRole {
  def apply(realmRole: String, error: ElementOrComponent, children: ElementOrComponent*): JsComponentConstructor =
    RawComponent(Seq(J("realmRole", realmRole), J("error", error)) ++ children: _*)

  private object RawComponent extends JsComponent(aas.RealmRole)
}
