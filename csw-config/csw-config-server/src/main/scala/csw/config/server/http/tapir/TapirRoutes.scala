package csw.config.server.http.tapir

import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.config.api.scaladsl.ConfigService
import csw.config.server.svn.SvnConfigServiceFactory
import sttp.tapir.server.akkahttp.RichAkkaHttpEndpoint

import scala.concurrent.ExecutionContext

class TapirRoutes(configServiceFactory: SvnConfigServiceFactory, securityDirectives: SecurityDirectives)(implicit
    ec: ExecutionContext
) {
  import Endpoints._

  private val UnknownUser = "Unknown"
  private val AdminRole   = "config-admin"

  private def configService(userName: String = UnknownUser): ConfigService = configServiceFactory.make(userName)

  val getConfig: Route = getConfigEndpoint.toRoute {
    case (path, dateParam, idParam) =>
      (dateParam, idParam) match {
        case (Some(date), _) => configService().getByTime(path, date).map(_.toRight(()))
        case (_, Some(id))   => configService().getById(path, id).map(_.toRight(()))
        case (_, _)          => configService().getLatest(path).map(_.toRight(()))
      }
  }

  val exist: Route = existEndpoint.toRoute {
    case (path, idParam) => configService().exists(path, idParam).map(Either.cond(_, (), ()))
  }

  val create: Route =
    securityDirectives.sPost(RealmRolePolicy(AdminRole)) { token =>
      createConfigEndpoint.toDirective {
        case ((path, _, annexParam, commentParam, configData), completion) =>
          completion(
            configService(token.userOrClientName).create(path, configData, annexParam, commentParam).map(Right(_))
          )
      }
    }

  val put: Route =
    securityDirectives.sPut(RealmRolePolicy(AdminRole)) { token =>
      putConfigEndpoint.toDirective {
        case ((path, _, commentParam, configData), completion) =>
          completion(configService(token.userOrClientName).update(path, configData, commentParam).map(_ => Right(())))
      }
    }

  val delete: Route =
    securityDirectives.sDelete(RealmRolePolicy(AdminRole)) { token =>
      deleteConfigEndpoint.toDirective {
        case ((path, _, commentParam), completion) =>
          completion(configService(token.userOrClientName).delete(path, commentParam).map(_ => Right(())))
      }
    }
}
