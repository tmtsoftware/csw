package csw.contract.data

import csw.contract.generator.models.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "location" -> Service(
        location.Models.httpEndpoints,
        location.Models.webSocketEndpoints,
        location.Models.models
      ),
      "command" -> Service(
        command.Models.httpEndpoints,
        command.Models.webSocketsEndpoints,
        command.Models.models
      )
    )
  )
}
