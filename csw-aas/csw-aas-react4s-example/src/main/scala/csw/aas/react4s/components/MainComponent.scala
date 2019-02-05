package csw.aas.react4s.components

import com.github.ahnfelt.react4s._
import csw.aas.react4s.facade.components._
import csw.aas.react4s.facade.config.Config

case class MainComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = {
    E.div(
      AuthContextProvider(
        J("config", Config),
        E.h1(Text("TMT Scala.js Application")),
        E.h3(Text("CheckLogin Component:")),
        CheckLogin(
          error = Login(),
          children = Logout()
        ),
        RealmRole(
          realmRole = "example-admin-role",
          error = Component(ErrorComponent),
          children = AuthContext.consume(ctx ⇒ {
            println("*" * 80)
            println("hasRealmRole: " + ctx.auth.hasRealmRole("example-admin-role"))
            println(ctx.auth.token())
            println("realmAccess: " + ctx.auth.realmAccess().roles)
            println("resourceAccess: " + ctx.auth.resourceAccess().values)
            println("hasResourceRole: " + ctx.auth.hasResourceRole("example-admin-role", None))
            val token = ctx.auth.tokenParsed()
            println("exp: " + token.exp)
            println("iat: " + token.iat)
            println("nonce: " + token.nonce)
            println("sub: " + token.sub)
            println("state: " + token.session_state)
            println("roles: " + token.realm_access.roles)
            println("resource_access: " + token.resource_access.values)
            println("*" * 80)

            Component(DummyComponent)
          }),
        ),
      )
    )
  }
}
