package csw.contract.data

import csw.contract.data.command.CommandContract
import csw.contract.data.location.LocationContract
import csw.contract.generator
import csw.contract.generator.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "Location-Service" -> Service(
        LocationContract.httpEndpoints,
        LocationContract.webSocketEndpoints,
        LocationContract.requests,
        LocationContract.models
      ),
      "Command-Service" -> generator.Service(
        CommandContract.httpEndpoints,
        CommandContract.webSocketsEndpoints,
        CommandContract.requests,
        CommandContract.models
      )
    )
  )
}
