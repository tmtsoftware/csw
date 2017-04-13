package csw.services.config.client.javadsl

import csw.services.config.api.javadsl.IConfigService
import csw.services.config.client.internal.ClientWiring

object JConfigClientFactory {

  def make(): IConfigService = {
    val clientWiring = new ClientWiring
    clientWiring.javaConfigService
  }

}
