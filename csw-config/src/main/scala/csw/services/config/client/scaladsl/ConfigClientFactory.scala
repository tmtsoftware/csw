package csw.services.config.client.scaladsl

import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ClientWiring

object ConfigClientFactory {

  def make(): ConfigService = {
    val clientWiring = new ClientWiring
    clientWiring.configService
  }

}
