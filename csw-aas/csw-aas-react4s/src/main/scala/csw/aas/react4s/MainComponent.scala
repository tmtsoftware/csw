package csw.aas.react4s

import com.github.ahnfelt.react4s._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

case class MainComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {

    val childrenProp: Node = E.div(Text("Error"))
    val error              = Error(J("children", childrenProp))
    E.div(
      E.nav(
        A.className("indigo"),
        E.div(
          A.className("nav-wrapper"),
          E.a(
            A.href("#"),
            A.className("brand-logo"),
            Text("TMT")
          ),
          E.ul(
            A.className("right hide-on-med-and-down"),
            E.li(
              E.a(
                A.href("https://github.com/tmtsoftware/esw-prototype"),
                Text("Github")
              )
            )
          )
        )
      ),
      E.div(error)
    )
  }
}
@js.native
@JSImport("csw-aas-js", JSImport.Namespace)
object AAS extends js.Object {
  val Logout: js.Dynamic                 = js.native
  val Login: js.Dynamic                  = js.native
  val CheckLogin: js.Dynamic             = js.native
  val RealmRole: js.Dynamic              = js.native
  val ResourceRole: js.Dynamic           = js.native
  val Error: js.Dynamic                  = js.native
  val TMTAuthContextProvider: js.Dynamic = js.native
  val Consumer: js.Dynamic               = js.native
}

object Error extends JsComponent(AAS.Error)
