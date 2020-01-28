package csw.contract.data.location

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.Done
import csw.contract.generator.models.ClassNameHelpers._
import csw.contract.generator.models.{Endpoint, ModelAdt}
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models._
import csw.location.models.codecs.LocationCodecs
import csw.prefix.models.{Prefix, Subsystem}
import csw.contract.generator.models.DomHelpers._
import scala.concurrent.duration.FiniteDuration

object Models extends LocationCodecs with LocationServiceCodecs {
  val port                     = 8080
  val prefix: Prefix           = Prefix(Subsystem.TCS, "filter.wheel")
  val componentId: ComponentId = ComponentId(prefix, ComponentType.HCD)

  val akkaConnection: AkkaConnection = AkkaConnection(componentId)
  val httpConnection: HttpConnection = HttpConnection(componentId)
  val tcpConnection: TcpConnection   = TcpConnection(componentId)
  val connectionInfo: ConnectionInfo = ConnectionInfo(prefix, ComponentType.HCD, ConnectionType.AkkaType)

  val akkaRegistration: Registration = AkkaRegistration(akkaConnection, new URI("some_path"))
  val httpRegistration: Registration = HttpRegistration(httpConnection, port, "path")
  val tcpRegistration: Registration  = TcpRegistration(tcpConnection, port)

  val akkaLocation: Location = AkkaLocation(akkaConnection, new URI("some_path"))
  val httpLocation: Location = HttpLocation(httpConnection, new URI("some_path"))
  val tcpLocation: Location  = TcpLocation(tcpConnection, new URI("some_path"))

  val registrationFailed: LocationServiceError        = RegistrationFailed("message")
  val otherLocationIsRegistered: LocationServiceError = OtherLocationIsRegistered("message")
  val unregisterFailed: LocationServiceError          = UnregistrationFailed(akkaConnection)
  val registrationListingFailed: LocationServiceError = RegistrationListingFailed()

  val locationUpdated: TrackingEvent = LocationUpdated(akkaLocation)
  val locationRemoved: TrackingEvent = LocationRemoved(akkaConnection)

  val seconds = 23

  val register: LocationHttpMessage             = Register(akkaRegistration)
  val unregister: LocationHttpMessage           = Unregister(httpConnection)
  val find: LocationHttpMessage                 = Find(akkaConnection)
  val resolve: LocationHttpMessage              = Resolve(akkaConnection, FiniteDuration(seconds, TimeUnit.SECONDS))
  val listByComponentType: LocationHttpMessage  = ListByComponentType(ComponentType.HCD)
  val listByHostname: LocationHttpMessage       = ListByHostname("hostname")
  val listByConnectionType: LocationHttpMessage = ListByConnectionType(ConnectionType.AkkaType)
  val listByPrefix: LocationHttpMessage         = ListByPrefix("TCS.filter.wheel")

  val track: LocationWebsocketMessage = Track(akkaConnection)

  val done: Done = Done

  val models: Map[String, ModelAdt] = Map(
    name[Registration]   -> ModelAdt(akkaRegistration, httpRegistration, tcpRegistration),
    name[Location]       -> ModelAdt(akkaLocation, httpLocation, tcpLocation),
    name[TrackingEvent]  -> ModelAdt(locationUpdated, locationRemoved),
    name[ConnectionType] -> ModelAdt.fromEnum(ConnectionType),
    name[ConnectionInfo] -> ModelAdt(connectionInfo),
    name[Connection]     -> ModelAdt(akkaConnection, httpConnection, tcpConnection),
    name[ComponentId]    -> ModelAdt(ComponentId(prefix, ComponentType.HCD)),
    name[ComponentType]  -> ModelAdt.fromEnum(ComponentType),
    name[LocationServiceError] -> ModelAdt(
      registrationFailed,
      otherLocationIsRegistered,
      unregisterFailed,
      registrationListingFailed
    ),
    name[LocationHttpMessage] -> ModelAdt(
      register,
      unregister,
      find,
      resolve,
      listByComponentType,
      listByHostname,
      listByConnectionType,
      listByPrefix
    ),
    name[LocationWebsocketMessage] -> ModelAdt(track),
    name[Subsystem]                -> ModelAdt.fromEnum(Subsystem),
    name[Prefix]                   -> ModelAdt(prefix)
  )

  val endpoints = List(
    Endpoint(name[Register], name[Location], List(name[RegistrationFailed])),
    Endpoint(name[Unregister], name[Done], List(name[UnregistrationFailed])),
    Endpoint(name[UnregisterAll.type], name[Done], List(name[UnregistrationFailed])),
    Endpoint(name[Find], optionName[Location], List()),
    Endpoint(name[Resolve], optionName[Location], List()),
    Endpoint(name[ListEntries.type], listName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByComponentType], listName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByConnectionType], listName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByHostname], listName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByPrefix], listName[Location], List(name[RegistrationListingFailed]))
  )
}
