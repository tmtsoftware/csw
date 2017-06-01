package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.location.commons.ClusterAwareSettings

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialise AdminServer
 */
class AdminHttpService(adminRoutes: AdminRoutes, actorRuntime: ActorRuntime) extends ClusterSeedLogger.Simple {

  import actorRuntime._

  lazy val registeredLazyBinding: Future[ServerBinding] = async {
    val binding = await(bind())
    log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    binding
  } recoverWith {
    case NonFatal(ex) ⇒
      log.error("can not start admin http server", ex)
      shutdown().map(_ ⇒ throw ex)
  }

  private def bind() = Http().bindAndHandle(
    handler = adminRoutes.route,
    interface = ClusterAwareSettings.hostname,
    port = 7878
  )
}
