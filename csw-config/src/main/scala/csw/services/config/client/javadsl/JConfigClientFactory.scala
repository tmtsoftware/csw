package csw.services.config.client.javadsl

import akka.actor.ActorSystem
import csw.services.config.api.javadsl.IConfigService
import csw.services.config.client.internal.ClientWiring
import csw.services.location.javadsl.ILocationService

object JConfigClientFactory {

  def make(): IConfigService = new ClientWiring().configService.asJava

  def make(actorSystem: ActorSystem): IConfigService = ClientWiring.make(actorSystem).configService.asJava

  def make(locationService: ILocationService): IConfigService = ClientWiring.make(locationService.asScala).configService.asJava

  def make(actorSystem: ActorSystem, locationService: ILocationService): IConfigService = {
    ClientWiring.make(actorSystem, locationService.asScala).configService.asJava
  }

}
