package csw.config.client.scaladsl

import akka.actor.ActorSystem
import csw.config.api.TokenFactory
import csw.location.api.scaladsl.LocationService
import csw.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.config.client.internal.{ActorRuntime, ConfigClient, ConfigServiceResolver}

/**
 * The factory is used to create ConfigClient instance.
 */
object ConfigClientFactory {

  /**
   * Create ConfigClient instance for admin users.
   *
   * @param actorSystem local actor system of the client
   * @param locationService location service instance which will be used to resolve the location of config server
   * @param tokenFactory factory to get access tokens
   * @return an instance of ConfigService
   */
  // Fixme: token factory should be mandatory for admin api, hence delete above admin api where tokenFactory is optional
  def adminApi(
      actorSystem: ActorSystem,
      locationService: LocationService,
      tokenFactory: TokenFactory
  ): ConfigService = make(new ActorRuntime(actorSystem), locationService, Some(tokenFactory))

  /**
   * Create ConfigClient instance for non admin users.
   *
   * @param actorSystem local actor system of the client
   * @param locationService location service instance which will be used to resolve the location of config server
   * @return an instance of ConfigClientService
   */
  def clientApi(actorSystem: ActorSystem, locationService: LocationService): ConfigClientService =
    make(new ActorRuntime(actorSystem), locationService)

  private[config] def make(
      actorRuntime: ActorRuntime,
      locationService: LocationService,
      factory: Option[TokenFactory] = None
  ): ConfigService = {
    val configServiceResolver = new ConfigServiceResolver(locationService, actorRuntime)
    new ConfigClient(configServiceResolver, actorRuntime, factory)
  }
}
