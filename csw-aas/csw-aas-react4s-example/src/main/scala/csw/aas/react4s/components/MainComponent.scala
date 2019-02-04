package csw.aas.react4s.components

import com.github.ahnfelt.react4s._
import csw.aas.react4s.facade.components._
import csw.aas.react4s.facade.config.Config

case class MainComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {
    E.div(
      TMTAuthContextProvider(
        J("config", Config),
        E.h1(Text("TMT Scala.js Application")),
        E.h3(Text("CheckLogin Component:")),
        CheckLogin(
          error = Login(),
          children = Logout()
        ),
        E.h3(Text("RealmRole Component:")),
        RealmRole(
          realmRole = "example-admin-role",
          error = Component(ErrorComponent),
          children = Component(DummyComponent)
        ),
      )
    )
  }
}
