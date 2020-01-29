package csw.contract.data

import csw.contract.generator.models.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "location" -> Service(
        location.LocationData.httpEndpoints,
        location.LocationData.webSocketEndpoints,
        location.LocationData.models
      ),
      "command" -> Service(
        command.CommandData.httpEndpoints,
        command.CommandData.webSocketsEndpoints,
        command.CommandData.models
      )
    )
  )
}
