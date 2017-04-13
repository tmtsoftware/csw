package csw.services.config.client.scaladsl

import akka.actor.ActorSystem
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ClientWiring

object ConfigClientFactory {

  def make(): ConfigService = ClientWiring.make().configService

  def make(actorSystem: ActorSystem): ConfigService = ClientWiring.make(actorSystem).configService

}
