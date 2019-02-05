package csw.aas.react4s.facade.components
import com.github.ahnfelt.react4s._
import csw.aas.react4s.facade.components.mapper.aas

object RealmRole {
  def apply(realmRole: String, error: ElementOrComponent, children: JsTag*): JsComponentConstructor =
    RawComponent(Seq(J("realmRole", realmRole), J("error", error)) ++ children: _*)

  private object RawComponent extends JsComponent(aas.RealmRole)
}
