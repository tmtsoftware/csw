package csw.aas.react4s

import com.github.ahnfelt.react4s._

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSImport

class Config extends js.Object {
  var realm: String    = _
  var clientId: String = _
}

case class SampleComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {
    E.div(Text("SampleComponent"))
  }
}

case class MainComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {
    val config = new Config()
    config.realm = "example"
    config.clientId = "example-app"
    val TMTProvider = TMTAuthContextProvider(J("config", JSON.parse(JSON.stringify(config))))
    E.div(
      TMTAuthContextProvider(
        J("config", JSON.parse(JSON.stringify(config))),
        E.div(Text("************text*************")),
        Component(SampleComponent),
        Login()
      )
    )
  }
}
@js.native
@JSImport("csw-aas-js1", JSImport.Namespace)
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

object Error                  extends JsComponent(AAS.Error)
object TMTAuthContextProvider extends JsComponent(AAS.TMTAuthContextProvider)
object Login                  extends JsComponent(AAS.Login)
