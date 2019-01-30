package csw.aas.react4s.facade
import com.github.ahnfelt.react4s.JsComponent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

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
object Logout                 extends JsComponent(AAS.Logout)
object CheckLogin             extends JsComponent(AAS.CheckLogin)
object RealmRole              extends JsComponent(AAS.RealmRole)
