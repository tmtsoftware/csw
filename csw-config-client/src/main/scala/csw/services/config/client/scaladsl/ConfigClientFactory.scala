package csw.services.config.client.scaladsl

import akka.actor.ActorSystem
import csw.services.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.services.config.client.internal.{ActorRuntime, ConfigClient, ConfigServiceResolver}
import csw.services.location.scaladsl.LocationService

/**
 * The factory is used to create ConfigClient instance.
 */
object ConfigClientFactory {

  /**
   * Create ConfigClient instance for admin users.
   *
   * @param actorSystem        local actor system of the client
   * @param locationService    location service instance which will be used to resolve the location of config server
   */
  def adminApi(actorSystem: ActorSystem, locationService: LocationService): ConfigService = {
    val actorRuntime          = new ActorRuntime(actorSystem)
    val configServiceResolver = new ConfigServiceResolver(locationService, actorRuntime)
    new ConfigClient(configServiceResolver, actorRuntime)
  }

  /**
   * Create ConfigClient instance for non admin users.
   *
   * @param actorSystem        local actor system of the client
   * @param locationService    location service instance which will be used to resolve the location of config server
   */
  def clientApi(actorSystem: ActorSystem, locationService: LocationService): ConfigClientService =
    adminApi(actorSystem, locationService)
}
