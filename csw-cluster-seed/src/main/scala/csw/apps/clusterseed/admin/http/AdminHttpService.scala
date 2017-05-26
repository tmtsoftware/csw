package csw.apps.clusterseed.admin.http

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialises ConfigServer
 * @param locationService          LocationService instance to be used for registering this server with the location service
 */
class AdminHttpService(
    locationService: LocationService,
    adminRoutes: AdminRoutes,
    actorRuntime: ActorRuntime
) {

  import actorRuntime._

  // this task needs to be added before calling register
  // so that location service shutdowns properly even in case of registration fails
  coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseServiceUnbind,
    "location-service-shutdown"
  )(() ⇒ locationService.shutdown())

  lazy val registeredLazyBinding: Future[ServerBinding] = async {
    val binding = await(bind())

//    println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    binding
  } recoverWith {
    case NonFatal(ex) ⇒ shutdown().map(_ ⇒ throw ex)
  }

  def shutdown(): Future[Done] = coordinatedShutdown.run()

  private def bind() = Http().bindAndHandle(
    handler = adminRoutes.route,
    interface = ClusterAwareSettings.hostname,
    port = 7878
  )
}
