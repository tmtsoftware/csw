package csw.contract.data.location.endpoints

import java.util.concurrent.TimeUnit

import akka.Done
import csw.contract.data.location.models.Instances._
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.Endpoint
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.models.{ComponentType, ConnectionType, Location, TypedConnection}
import msocket.api.codecs.BasicCodecs

import scala.concurrent.duration.FiniteDuration

object Instances extends LocationServiceCodecs with BasicCodecs {

  private val seconds                   = 23
  val registerAkka: LocationHttpMessage = Register(akkaRegistration)
  val registerHttp: LocationHttpMessage = Register(httpRegistration)
  val unregister: LocationHttpMessage   = Unregister(httpConnection)
  val find: LocationHttpMessage         = Find(akkaConnection.asInstanceOf[TypedConnection[Location]])
  val resolve: LocationHttpMessage =
    Resolve(akkaConnection.asInstanceOf[TypedConnection[Location]], FiniteDuration(seconds, TimeUnit.SECONDS))
  val listByComponentType: LocationHttpMessage  = ListByComponentType(ComponentType.HCD)
  val listByHostname: LocationHttpMessage       = ListByHostname("hostname")
  val listByConnectionType: LocationHttpMessage = ListByConnectionType(ConnectionType.AkkaType)
  val listByPrefix: LocationHttpMessage         = ListByPrefix("TCS.filter.wheel")
  val track: LocationWebsocketMessage           = Track(akkaConnection)

  val done: Done                = Done
  val option: Option[Location]  = Some(akkaLocation)
  val locations: List[Location] = List(akkaLocation, httpLocation)

  val endpoints: Map[String, Endpoint] = Map(
    "register" -> Endpoint(
      requests = List(registerAkka, registerHttp),
      responses = List(registrationFailed, akkaLocation)
    ),
    "unregister" -> Endpoint(
      requests = List(unregister),
      responses = List(done)
    ),
    "unregisterAll" -> Endpoint(
      requests = List(),
      responses = List(done)
    ),
    "find" -> Endpoint(
      requests = List(find),
      responses = List(option)
    ),
    "resolve" -> Endpoint(
      requests = List(resolve),
      responses = List(option)
    ),
    "listEntries" -> Endpoint(
      requests = List(),
      responses = List(locations)
    ),
    "listByComponentType" -> Endpoint(
      requests = List(listByComponentType),
      responses = List(locations)
    ),
    "listByHostname" -> Endpoint(
      requests = List(listByHostname),
      responses = List(locations)
    ),
    "listByConnectionType" -> Endpoint(
      requests = List(listByConnectionType),
      responses = List(locations)
    ),
    "listByPrefix" -> Endpoint(
      requests = List(listByPrefix),
      responses = List(locations)
    ),
    "track" -> Endpoint(
      requests = List(track),
      responses = List(locationRemoved, locationUpdated)
    )
  )
}
