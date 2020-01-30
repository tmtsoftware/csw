package csw.contract.data

import csw.contract.data.command.CommandContract
import csw.contract.data.location.LocationContract
import csw.contract.generator.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "Location-Service" -> Service(
        LocationContract.http,
        LocationContract.webSockets,
        LocationContract.models
      ),
      "Command-Service" -> Service(
        CommandContract.http,
        CommandContract.webSockets,
        CommandContract.models
      )
    )
  )
}
