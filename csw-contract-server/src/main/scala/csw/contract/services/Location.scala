package csw.contract.services

import java.net.URI

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed}
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage.Register
import csw.location.models.Connection.AkkaConnection
import csw.location.models._
import csw.prefix.models.Prefix
import io.bullet.borer.Json
import play.api.libs.json
import play.api.libs.json.{JsArray, JsObject}

object Location extends LocationServiceCodecs {
  private val componentId                           = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
  private val akkaConnection                        = AkkaConnection(componentId)
  private val akkaRegistration: LocationHttpMessage = Register(AkkaRegistration(akkaConnection, new URI("some_path")))

  private val akkaLocation: Location                   = AkkaLocation(akkaConnection, new URI("some_path"))
  private val registrationFailed: LocationServiceError = RegistrationFailed("message")
  private val akkaRequest                              = json.Json.parse(Json.encode(akkaRegistration).toUtf8String)
  private val akkaSuccessResponse                      = json.Json.parse(Json.encode(akkaLocation).toUtf8String)
  private val registrationFailedError                  = json.Json.parse(Json.encode(registrationFailed).toUtf8String)

  val contractSamples: JsObject = JsObject(
    Map("requests" -> JsArray(List(akkaRequest)), "responses" -> JsArray(List(akkaSuccessResponse, registrationFailedError)))
  )
}
