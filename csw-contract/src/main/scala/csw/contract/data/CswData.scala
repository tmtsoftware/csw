package csw.contract.data

import csw.contract.data.command.CommandContract
import csw.contract.data.location.LocationContract
import csw.contract.generator.models.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "location" -> Service(
        LocationContract.httpEndpoints,
        LocationContract.webSocketEndpoints,
        LocationContract.models
      ),
      "command" -> Service(
        CommandContract.httpEndpoints,
        CommandContract.webSocketsEndpoints,
        CommandContract.models
      )
    )
  )
}
