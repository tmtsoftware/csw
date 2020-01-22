package csw.contract.data.command.endpoints

import akka.Done
import csw.contract.data.location.models.Instances._
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.Endpoint
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage.{Register, Unregister}
import msocket.api.codecs.BasicCodecs

object Instances extends LocationServiceCodecs with BasicCodecs {

  val registerAkka: LocationHttpMessage = Register(akkaRegistration)
  val registerHttp: LocationHttpMessage = Register(httpRegistration)
  val unregister: LocationHttpMessage   = Unregister(connection)
  val done: Done                        = Done

  val endpoints: Map[String, Endpoint] = Map(
    "register" -> Endpoint(
      requests = List(registerAkka, registerHttp),
      responses = List(registrationFailed, akkaLocation)
    ),
    "unregister" -> Endpoint(
      requests = List(unregister),
      responses = List(done)
    )
  )
}
