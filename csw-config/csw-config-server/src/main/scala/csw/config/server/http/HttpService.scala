package csw.config.server.http

import java.net.BindException

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.config.server.commons.{ConfigServerLogger, ConfigServiceConnection}
import csw.config.server.{ActorRuntime, Settings}
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.{Networks, SocketUtils}

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialises ConfigServer at given port and register with location service
 *
 * @param locationService locationService instance to be used for registering this server with the location service
 * @param configServiceRoute configServiceRoute instance representing the routes supported by this server
 * @param settings runtime configuration of server
 * @param actorRuntime actorRuntime instance wrapper for actor system
 */
class HttpService(
    locationService: LocationService,
    configServiceRoute: ConfigServiceRoute,
    settings: Settings,
    actorRuntime: ActorRuntime
) {

  import actorRuntime.{coordinatedShutdown, ec, typedSystem}

  private val log: Logger = ConfigServerLogger.getLogger

  lazy val registeredLazyBinding: Future[(ServerBinding, RegistrationResult)] = async {
    val binding            = await(bind())            // create HttpBinding with appropriate hostname and port
    val registrationResult = await(register(binding)) // create HttpRegistration and register it with location service

    // Add the task to unregister the HttpRegistration from location service.
    // This will execute as the first task out of all tasks at the shutdown of ActorSystem.
    // ActorSystem will shutdown if any SVNException is thrown while running the ConfigService app (refer Main.scala)
    // If for some reason ActorSystem is not shutdown gracefully then a jvm shutdown hook is in place which will unregister
    // HttpRegistration from location service.
    // And if for some reason jvm shutdown hook does not get executed then the DeathWatchActor running in cluster will get notified that config service is down
    // and it will unregister HttpRegistration from location service.
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())

    log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    (binding, registrationResult)
  } recoverWith {
    case NonFatal(ex) => shutdown().map(_ => throw ex)
  }

  def shutdown(): Future[Done] = actorRuntime.shutdown()

  private def bind() = {

    val _host = Networks(NetworkType.Public.envKey).hostname
    val _port = settings.`service-port`

    /*
      Check _host:_port is free. If we bind to 0.0.0.0:_port, then it will allow some other third party application/service
       to bind to _host:_port at later point of time. To avoid that we bind and register to specific _host:_port.
     */
    if (SocketUtils.isAddressInUse(_host, _port))
      throw new BindException(s"Bind failed for TCP channel on endpoint [${_host}:${_port}]. Address already in use.")

    Http().bindAndHandle(
      handler = configServiceRoute.route,
      interface = _host,
      port = _port
    )
  }

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = ConfigServiceConnection.value,
      port = binding.localAddress.getPort,
      path = "",
      NetworkType.Public
    )
    log.info(
      s"Registering Config Service HTTP Server with Location Service using registration: [${registration.toString}]"
    )
    locationService.register(registration)
  }
}
