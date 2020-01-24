package csw.contract.data.location.models

import java.net.URI

import csw.contract.generator.models.DomHelpers.encode
import csw.contract.generator.models.ModelAdt
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed}
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models._
import csw.location.models.codecs.LocationCodecs
import csw.prefix.models.Prefix

object Instances extends LocationCodecs {
  val componentId: ComponentId       = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
  val akkaConnection: AkkaConnection = AkkaConnection(componentId)
  val httpConnection: HttpConnection = HttpConnection(componentId)

  val akkaRegistration: Registration = AkkaRegistration(akkaConnection, new URI("some_path"))
  val httpRegistration: Registration = HttpRegistration(httpConnection, 8080, "paht1")

  val akkaLocation: Location                   = AkkaLocation(akkaConnection, new URI("some_path"))
  val httpLocation: Location                   = HttpLocation(httpConnection, new URI("some_path"))
  val registrationFailed: LocationServiceError = RegistrationFailed("message")
  val connection: HttpConnection               = HttpConnection(componentId)

  val models: Map[String, ModelAdt] = Map(
    "registration" -> ModelAdt(
      List(akkaRegistration, httpRegistration)
    ),
    "location" -> ModelAdt(
      List(akkaLocation, httpLocation)
    )
  )
}
