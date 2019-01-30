package csw.aas.react4s.components

import com.github.ahnfelt.react4s._
import csw.aas.react4s.config.Config
import csw.aas.react4s.facade.{CheckLogin, Login, Logout, TMTAuthContextProvider}

case class MainComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {
    E.div(
      TMTAuthContextProvider(
        J("config", Config),
        E.div(Text("TMT Scala.js Application")),
        Component(SampleComponent),
        CheckLogin(
          J("error", Login()),
          E.div(Logout())
        )
      )
    )
  }
}
