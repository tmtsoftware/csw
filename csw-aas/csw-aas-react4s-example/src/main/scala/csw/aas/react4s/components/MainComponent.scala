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
            printAuthContext(ctx)
            Component(DummyComponent)
          }),
        ),
      )
    )
  }

  private def printAuthContext(ctx: AuthContext): Unit = {
    println("*" * 80)
    println("hasRealmRole: " + ctx.auth.hasRealmRole("example-admin-role"))
    println("token: " + ctx.auth.token())
    println("realmAccess: " + ctx.auth.realmAccess().roles)
    println("resourceAccess: " + ctx.auth.resourceAccess().values)
    println("hasResourceRole: " + ctx.auth.hasResourceRole("example-admin-role", None))

    println("*" * 80)
    printParsedToken(ctx)
    println("*" * 80)
    printUserProfile(ctx)
    println("*" * 80)
  }

  private def printParsedToken(ctx: AuthContext) = {
    val tokenOpt = ctx.auth.tokenParsed()
    println("Printing Parsed Token: ")
    for {
      token ← tokenOpt
      exp   ← token.exp
      iat   ← token.iat
      nonce ← token.nonce
      sub   ← token.sub
      state ← token.session_state
    } yield {
      println("exp: " + exp)
      println("iat: " + iat)
      println("nonce: " + nonce)
      println("sub: " + sub)
      println("state: " + state)
      println("roles: " + token.realm_access.roles)
      println("resource_access: " + token.resource_access.values)
    }
  }

  private def printUserProfile(ctx: AuthContext) =
    ctx.auth
      .loadUserProfile()
      .success { info ⇒
        println("Printing User Profile: ")
        println("id: " + info.id)
        println("username: " + info.username)
        println("email: " + info.email)
        println("firstName: " + info.firstName)
        println("lastName: " + info.lastName)
        println("enabled: " + info.enabled)
        println("emailVerified: " + info.emailVerified)
        println("totp: " + info.totp)
        println("createdTimestamp: " + info.createdTimestamp)
      }

}
