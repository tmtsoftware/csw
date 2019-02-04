package csw.aas.react4s.facade.components
import com.github.ahnfelt.react4s.{ElementOrComponent, J, JsComponent, JsComponentConstructor}
import csw.aas.react4s.facade.components.mapper.aas

object ClientRole {
  def apply(
      clientRole: String,
      client: Option[String],
      error: ElementOrComponent,
      children: ElementOrComponent*
  ): JsComponentConstructor =
    RawComponent(Seq(J("clientRole", clientRole), J("client", client.orNull), J("error", error)) ++ children: _*)

  private object RawComponent extends JsComponent(aas.ClientRole)
}
