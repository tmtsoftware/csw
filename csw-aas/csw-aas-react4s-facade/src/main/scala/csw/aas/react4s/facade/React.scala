package csw.aas.react4s.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("react", JSImport.Namespace)
object React extends js.Object {
  def createClass(specification: js.Dictionary[js.Any]): js.Dynamic =
    js.native

  def createElement(
      element: js.Any,
      props: js.Dictionary[js.Any] = null,
      children: js.Any = null
  ): js.Dynamic = js.native

  def createContext(defaultValue: Any): ReactContext = js.native

  @js.native
  trait ReactContext extends js.Object {
    val Provider: js.Any
    val Consumer: js.Any
  }
}
