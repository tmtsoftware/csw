package example.auth

import akka.actor.typed
import akka.actor.typed.SpawnProtocol
import akka.http.scaladsl.server._
import com.typesafe.config.Config
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.SecurityDirectives
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.{ExecutionContext, Future}

object AuthDocumentation extends HttpApp {
  private implicit val actorSystem: typed.ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "")
  private implicit val ec: ExecutionContext                                  = actorSystem.executionContext

  private val config: Config  = actorSystem.settings.config
  private val locationService = HttpLocationServiceFactory.makeLocalClient
  private val directives      = SecurityDirectives(config, locationService)

  import directives._

  object Database {
    def doesUserOwnFile(userId: Option[String], fileId: Long): Future[Boolean] = ???

    def getFileContents(fileId: Long): String = ???
  }

  object ThirdPartyService {
    def deleteEntity(entityId: Long, username: Option[String]): Nothing = ???

    def deleteEntity(entityId: Long): Nothing = ???
  }

  // #access-token-handle-demo
  val routeExampleWithToken: Route = sDelete(EmptyPolicy) { token =>
    parameter("entityId".as[Long]) { entityId =>
      ThirdPartyService.deleteEntity(entityId, token.preferred_username)
      complete(s"user ${token.given_name} ${token.family_name} deleted entity $entityId")
    }
  }

  val routeExampleWithoutToken: Route = sDelete(EmptyPolicy) {
    parameter("entityId".as[Long]) { entityId =>
      ThirdPartyService.deleteEntity(entityId)
      complete(s"entity $entityId deleted")
    }
  }
  // #access-token-handle-demo

  object Policies {
    // #custom-policy-async
    //GET http://[host]:[port]/files?fileId=[fileId]
    val route: Route =
      path("files" / LongNumber) { fileId =>
        sGet(CustomPolicyAsync(token => Database.doesUserOwnFile(token.preferred_username, fileId))) {
          complete(Database.getFileContents(fileId))
        }
      }
    // #custom-policy-async

    // #empty-policy-usage
    val authenticationOnlyRoute: Route = // GET http://[host]:[post]/api
      path("api") {
        sGet(EmptyPolicy) {
          complete("OK")
        }
      }
    // #empty-policy-usage

    //#realm-role-policy-usage
    val routeWithRealmRolePolicy: Route = sGet(RealmRolePolicy("admin")) {
      complete("OK")
    }
    //#realm-role-policy-usage

    //#client-role-policy-usage
    val routeWithClientRolePolicy: Route = sGet(ClientRolePolicy("accounts-admin")) {
      complete("OK")
    }
    //#client-role-policy-usage

    // #custom-policy-usage
    val routeWithCustomPolicy: Route = sPost(CustomPolicy(token => token.given_name.contains("test-user"))) {
      complete("OK")
    }
    // #custom-policy-usage

    // #permission-policy
    val routeWithPermissions: Route = sDelete(PermissionPolicy("delete", "account")) {
      complete("OK")
    }
    // #permission-policy
  }

  object PolicyExpressions {
    // #policy-expressions
    val routes: Route =
      sGet(RealmRolePolicy("admin") | CustomPolicy(_.email.contains("super-admin@tmt.org"))) {
        complete("OK")
      } ~
        sPost(ClientRolePolicy("finance_user") & PermissionPolicy("edit")) {
          complete("OK")
        }
    // #policy-expressions
  }

  object DirectiveComposition {
    // #policy-expressions-right-way
    sGet(RealmRolePolicy("admin") & ClientRolePolicy("sales_admin"))
    // #policy-expressions-right-way

    // #directive-composition-anti-pattern
    sGet(RealmRolePolicy("admin")) & sGet(ClientRolePolicy("sales_admin"))
    // #directive-composition-anti-pattern
  }

  val routes: Route = ???
}
