package csw.aas.react4s.facade.components.mapper
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

// fixme: figure out better way to depend on csw-aas-js (consider dev and prod)
@js.native
@JSImport("../../../../../csw-aas-js/dist/index.js", JSImport.Namespace)
private[facade] object aas extends js.Object {
  val Login: js.Dynamic               = js.native
  val Logout: js.Dynamic              = js.native
  val CheckLogin: js.Dynamic          = js.native
  val RealmRole: js.Dynamic           = js.native
  val ClientRole: js.Dynamic          = js.native
  val AuthContextProvider: js.Dynamic = js.native
  val Consumer: js.Dynamic            = js.native
  val AuthContext: js.Dynamic         = js.native
}
