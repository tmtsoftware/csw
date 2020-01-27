package csw.contract.data

import csw.contract.generator.models.{Service, Services}

object CswData {
  val services: Services = Services(
    Map(
      "location" -> Service(
        location.endpoints.Instances.endpoints,
        location.models.Instances.models
      ),
      "command" -> Service(
        command.endpoints.Instances.endpoints,
        command.models.Instances.models
      )
    )
  )
}
