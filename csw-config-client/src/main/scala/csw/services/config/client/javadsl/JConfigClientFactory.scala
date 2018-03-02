package csw.services.config.client.javadsl

import akka.actor.ActorSystem
import csw.services.config.api.javadsl.{IConfigClientService, IConfigService}
import csw.services.config.client.internal.{ActorRuntime, JConfigService}
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.javadsl.ILocationService

/**
 * The factory is used to create ConfigClient instance.
 */
object JConfigClientFactory {

  /**
   * Create ConfigClient instance for admin users.
   *
   * @param actorSystem        Local actor system of the client
   * @param locationService    Location service instance which will be used to resolve the location of config server
   * @return                   An instance of IConfigService
   */
  def adminApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigService = {
    val configService = ConfigClientFactory.adminApi(actorSystem, locationService.asScala)
    val actorRuntime  = new ActorRuntime(actorSystem)
    new JConfigService(configService, actorRuntime)
  }

  /**
   * Create ConfigClient instance for non admin users.
   *
   * @param actorSystem        Local actor system of the client
   * @param locationService    Location service instance which will be used to resolve the location of config server
   * @return                   An instance of IConfigClientService
   */
  def clientApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigClientService =
    adminApi(actorSystem, locationService)

}
