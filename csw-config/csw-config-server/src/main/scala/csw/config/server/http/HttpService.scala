/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.http

import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import csw.config.server.commons.{ConfigServerLogger, ConfigServiceConnection}
import csw.config.server.{ActorRuntime, Settings}
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.Networks

import cps.*
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
  } recoverWith { case NonFatal(ex) =>
    shutdown().map(_ => throw ex)
  }

  def shutdown(): Future[Done] = actorRuntime.shutdown()

  private def bind() = {

    val _host = Networks(NetworkType.Outside.envKey).hostname
    val _port = settings.`service-port`

    Http().newServerAt(_host, _port).bind(configServiceRoute.route)
  }

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = ConfigServiceConnection.value,
      port = binding.localAddress.getPort,
      path = "",
      NetworkType.Outside
    )
    log.info(
      s"Registering Config Service HTTP Server with Location Service using registration: [${registration.toString}]"
    )
    locationService.register(registration)
  }
}
