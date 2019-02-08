package csw.aas.react4s.facade

import com.github.ahnfelt.react4s.ReactBridge
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NpmReactBridge extends ReactBridge(React, ReactDOM)

@js.native
@JSImport("react", JSImport.Namespace)
object React extends ReactBridge.React

@js.native
@JSImport("react-dom", JSImport.Namespace)
object ReactDOM extends js.Object
