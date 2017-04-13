package csw.services.config.client.javadsl

import akka.actor.ActorSystem
import csw.services.config.api.javadsl.IConfigService
import csw.services.config.client.internal.ClientWiring

object JConfigClientFactory {

  def make(): IConfigService = ClientWiring.make().javaConfigService

  def make(actorSystem: ActorSystem): IConfigService = ClientWiring.make(actorSystem).javaConfigService

}
