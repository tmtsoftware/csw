package csw.services.config.server.http

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.services.config.server.commons.ConfigServiceConnection
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService
import csw.services.config.server.commons.ConfigServerLogger
import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialises ConfigServer
 * @param locationService          LocationService instance to be used for registering this server with the location service
 * @param configServiceRoute       ConfigServiceRoute instance representing the routes supported by this server
 * @param settings                 Runtime configuration of server
 * @param actorRuntime             ActorRuntime instance wrapper for actor system
 */
class HttpService(locationService: LocationService,
                  configServiceRoute: ConfigServiceRoute,
                  settings: Settings,
                  actorRuntime: ActorRuntime)
    extends ConfigServerLogger.Simple {

  import actorRuntime._

  // this task needs to be added before calling register
  // so that location service shutdowns properly even in case of registration fails
  coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseServiceUnbind,
    "location-service-shutdown"
  )(() ⇒ locationService.shutdown())

  lazy val registeredLazyBinding: Future[(ServerBinding, RegistrationResult)] = async {
    val binding            = await(bind())
    val registrationResult = await(register(binding))

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())

    log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    (binding, registrationResult)
  } recoverWith {
    case NonFatal(ex) ⇒ shutdown().map(_ ⇒ throw ex)
  }

  def shutdown(): Future[Done] = actorRuntime.shutdown()

  private def bind() = Http().bindAndHandle(
    handler = configServiceRoute.route,
    interface = ClusterAwareSettings.hostname,
    port = settings.`service-port`
  )

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = ConfigServiceConnection,
      port = binding.localAddress.getPort,
      path = ""
    )
    log.info("==== Registering Config Service HTTP Server with Location Service ====")
    locationService.register(registration)
  }
}
