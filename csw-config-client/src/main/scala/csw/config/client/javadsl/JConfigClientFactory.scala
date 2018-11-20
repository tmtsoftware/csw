package csw.config.client.javadsl

import akka.actor.ActorSystem
import csw.config.api.TokenFactory
import csw.config.api.javadsl.{IConfigClientService, IConfigService}
import csw.config.client.internal.{ActorRuntime, JConfigService}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.javadsl.ILocationService

/**
 * The factory is used to create ConfigClient instance.
 */
object JConfigClientFactory {

  /**
   * Create ConfigClient instance for admin users.
   *
   * @param actorSystem local actor system of the client
   * @param locationService location service instance which will be used to resolve the location of config server
   * @return an instance of IConfigService
   */
  def adminApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigService = {
    val configService = ConfigClientFactory.adminApi(actorSystem, locationService.asScala)
    val actorRuntime  = new ActorRuntime(actorSystem)
    new JConfigService(configService, actorRuntime)
  }

  // Fixme: token factory should be mandatory for admin api, hence delete above admin api where tokenFactory is optional
  def adminApi(
      actorSystem: ActorSystem,
      locationService: ILocationService,
      tokenFactory: TokenFactory
  ): IConfigService = {
    val configService = ConfigClientFactory.adminApi(actorSystem, locationService.asScala, tokenFactory)
    val actorRuntime  = new ActorRuntime(actorSystem)
    new JConfigService(configService, actorRuntime)
  }

  /**
   * Create ConfigClient instance for non admin users.
   *
   * @param actorSystem local actor system of the client
   * @param locationService location service instance which will be used to resolve the location of config server
   * @return an instance of IConfigClientService
   */
  def clientApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigClientService =
    adminApi(actorSystem, locationService)

}
