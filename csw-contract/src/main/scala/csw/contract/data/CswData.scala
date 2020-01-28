package csw.contract.data

import csw.contract.data.location.Models._
import csw.contract.generator.models.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "location" -> Service(
        endpoints,
        models
      ),
      "command" -> Service(
        command.endpoints.Instances.endpoints,
        command.models.Instances.models
      )
    )
  )
}
