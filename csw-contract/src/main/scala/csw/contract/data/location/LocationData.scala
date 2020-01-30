package csw.contract.data.location

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.Done
import csw.contract.generator.models.ClassNameHelpers._
import csw.contract.generator.models.DomHelpers._
import csw.contract.generator.models.{Endpoint, ModelType}
import csw.location.api.codec.{LocationCodecs, LocationServiceCodecs}
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

object LocationData extends LocationCodecs with LocationServiceCodecs {
  val port       = 8080
  val seconds    = 23
  val pathString = "path"
  val uriPath    = new URI("path")
  val message    = "failure message"
  val hostname   = "hostname"
  val done: Done = Done

  val prefix: Prefix           = Prefix(Subsystem.TCS, "filter.wheel")
  val componentId: ComponentId = ComponentId(prefix, ComponentType.HCD)

  val akkaConnection: AkkaConnection = AkkaConnection(componentId)
  val httpConnection: HttpConnection = HttpConnection(componentId)
  val tcpConnection: TcpConnection   = TcpConnection(componentId)
  val connectionInfo: ConnectionInfo = ConnectionInfo(prefix, ComponentType.HCD, ConnectionType.AkkaType)

  val akkaRegistration: Registration = AkkaRegistration(akkaConnection, uriPath)
  val httpRegistration: Registration = HttpRegistration(httpConnection, port, pathString)
  val tcpRegistration: Registration  = TcpRegistration(tcpConnection, port)

  val akkaLocation: Location = AkkaLocation(akkaConnection, uriPath)
  val httpLocation: Location = HttpLocation(httpConnection, uriPath)
  val tcpLocation: Location  = TcpLocation(tcpConnection, uriPath)

  val registrationFailed: LocationServiceError        = RegistrationFailed(message)
  val otherLocationIsRegistered: LocationServiceError = OtherLocationIsRegistered(message)
  val unregisterFailed: LocationServiceError          = UnregistrationFailed(akkaConnection)
  val registrationListingFailed: LocationServiceError = RegistrationListingFailed()

  val locationUpdated: TrackingEvent = LocationUpdated(akkaLocation)
  val locationRemoved: TrackingEvent = LocationRemoved(akkaConnection)

  val register: LocationHttpMessage             = Register(akkaRegistration)
  val unregister: LocationHttpMessage           = Unregister(httpConnection)
  val unregisterAll: LocationHttpMessage        = UnregisterAll
  val find: LocationHttpMessage                 = Find(akkaConnection)
  val resolve: LocationHttpMessage              = Resolve(akkaConnection, FiniteDuration(seconds, TimeUnit.SECONDS))
  val listEntries: LocationHttpMessage          = ListEntries
  val listByComponentType: LocationHttpMessage  = ListByComponentType(ComponentType.HCD)
  val listByHostname: LocationHttpMessage       = ListByHostname(hostname)
  val listByConnectionType: LocationHttpMessage = ListByConnectionType(ConnectionType.AkkaType)
  val listByPrefix: LocationHttpMessage         = ListByPrefix(prefix.toString)

  val track: LocationWebsocketMessage = Track(akkaConnection)

  val models: Map[String, ModelType] = Map(
    name[Registration]   -> ModelType(akkaRegistration, httpRegistration, tcpRegistration),
    name[Location]       -> ModelType(akkaLocation, httpLocation, tcpLocation),
    name[TrackingEvent]  -> ModelType(locationUpdated, locationRemoved),
    name[ConnectionType] -> ModelType(ConnectionType),
    name[ConnectionInfo] -> ModelType(connectionInfo),
    name[Connection]     -> ModelType(akkaConnection, httpConnection, tcpConnection),
    name[ComponentId]    -> ModelType(ComponentId(prefix, ComponentType.HCD)),
    name[ComponentType]  -> ModelType(ComponentType),
    name[LocationServiceError] -> ModelType(
      registrationFailed,
      otherLocationIsRegistered,
      unregisterFailed,
      registrationListingFailed
    ),
    name[Subsystem] -> ModelType(Subsystem),
    name[Prefix]    -> ModelType(prefix)
  )

  val httpEndpoints = Map(
    name[Register]             -> Endpoint(register, name[Location], List(name[RegistrationFailed])),
    name[Unregister]           -> Endpoint(unregister, name[Done], List(name[UnregistrationFailed])),
    objectName(UnregisterAll)  -> Endpoint(unregisterAll, name[Done], List(name[UnregistrationFailed])),
    name[Find]                 -> Endpoint(find, arrayName[Location]),
    name[Resolve]              -> Endpoint(resolve, arrayName[Location]),
    objectName(ListEntries)    -> Endpoint(listEntries, arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByComponentType]  -> Endpoint(listByComponentType, arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByConnectionType] -> Endpoint(listByConnectionType, arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByHostname]       -> Endpoint(listByHostname, arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByPrefix]         -> Endpoint(listByPrefix, arrayName[Location], List(name[RegistrationListingFailed]))
  )

  val webSocketEndpoints = Map(
    name[Track] -> Endpoint(track, name[TrackingEvent])
  )
}
