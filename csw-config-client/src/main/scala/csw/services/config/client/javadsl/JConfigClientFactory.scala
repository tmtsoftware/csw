package csw.services.config.client.javadsl

import akka.actor.ActorSystem
import csw.services.config.api.javadsl.{IConfigClientService, IConfigService}
import csw.services.config.client.internal.{ActorRuntime, JConfigService}
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.javadsl.ILocationService

object JConfigClientFactory {

  def adminApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigService = {
    val configService = ConfigClientFactory.adminApi(actorSystem, locationService.asScala)
    val actorRuntime  = new ActorRuntime(actorSystem)
    new JConfigService(configService, actorRuntime)
  }

  def clientApi(actorSystem: ActorSystem, locationService: ILocationService): IConfigClientService =
    adminApi(actorSystem, locationService)

}
