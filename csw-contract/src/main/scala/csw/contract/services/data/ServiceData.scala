package csw.contract.services.data

import csw.contract.services.models.{Service, Services}

object ServiceData {
  val data: Services = Services(
    Map(
      "location" -> Service(
        location.endpoints.Instances.endpoints,
        location.models.Instances.models,
      ),
      "command" -> Service(
        command.endpoints.Instances.endpoints,
        command.models.Instances.models,
      )
    )
  )
}
