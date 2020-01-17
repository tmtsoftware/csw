package csw.contract.services

import java.net.URI

import akka.Done
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed}
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage.{Register, Unregister}
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models._
import csw.prefix.models.Prefix
import io.bullet.borer.Json
import msocket.api.codecs.BasicCodecs
import play.api.libs.json
import play.api.libs.json.{JsArray, JsObject}

object Location {
  val registerSamples: JsObject = RegisterSamples.samples;
  val unRegisterSamples: JsObject = UnregisterSamples.samples;
}

object RegisterSamples extends LocationServiceCodecs {
  private val componentId                           = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
  private val akkaConnection                        = AkkaConnection(componentId)
  private val akkaRegistration: LocationHttpMessage = Register(AkkaRegistration(akkaConnection, new URI("some_path")))

  private val akkaLocation: Location                   = AkkaLocation(akkaConnection, new URI("some_path"))
  private val registrationFailed: LocationServiceError = RegistrationFailed("message")
  private val akkaRequest                              = json.Json.parse(Json.encode(akkaRegistration).toUtf8String)
  private val akkaSuccessResponse                      = json.Json.parse(Json.encode(akkaLocation).toUtf8String)
  private val registrationFailedError                  = json.Json.parse(Json.encode(registrationFailed).toUtf8String)

  val samples: JsObject = JsObject(
    Map("requests" -> JsArray(List(akkaRequest)), "responses" -> JsArray(List(akkaSuccessResponse, registrationFailedError)))
  )
}

object UnregisterSamples extends LocationServiceCodecs with BasicCodecs {
  private val componentId                     = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
  private val connection                      = HttpConnection(componentId)
  private val unregister: LocationHttpMessage = Unregister(connection)
  private val done: Done                      = Done
  private val request                         = json.Json.parse(Json.encode(unregister).toUtf8String)
  private val successResponse                 = json.Json.parse(Json.encode(done).toUtf8String)

  val samples: JsObject = JsObject(
    Map("requests" -> JsArray(List(request)), "responses" -> JsArray(List(successResponse)))
  )
}
