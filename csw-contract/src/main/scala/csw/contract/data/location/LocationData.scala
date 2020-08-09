package csw.contract.data.location

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.Done
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

trait LocationData {
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

  val metadata: Metadata = Metadata(Map("key1" -> "value"))

  val akkaRegistration: Registration       = AkkaRegistration(akkaConnection, uriPath, metadata)
  val httpRegistration: Registration       = HttpRegistration(httpConnection, port, pathString)
  val publicHttpRegistration: Registration = HttpRegistration(httpConnection, port, pathString, NetworkType.Public)
  val tcpRegistration: Registration        = TcpRegistration(tcpConnection, port)

  val akkaLocation: Location = AkkaLocation(akkaConnection, uriPath, metadata)
  val httpLocation: Location = HttpLocation(httpConnection, uriPath, Metadata.empty)
  val tcpLocation: Location  = TcpLocation(tcpConnection, uriPath, Metadata.empty)

  val registrationFailed: LocationServiceError        = RegistrationFailed(message)
  val otherLocationIsRegistered: LocationServiceError = OtherLocationIsRegistered(message)
  val unregisterFailed: LocationServiceError          = UnregistrationFailed(akkaConnection)
  val registrationListingFailed: LocationServiceError = RegistrationListingFailed()

  val locationUpdated: TrackingEvent = LocationUpdated(akkaLocation)
  val locationRemoved: TrackingEvent = LocationRemoved(akkaConnection)

  val akkaRegister: Register                           = Register(akkaRegistration)
  val httpRegister: Register                           = Register(httpRegistration)
  val publicHttpRegister: Register                     = Register(publicHttpRegistration)
  val unregister: Unregister                           = Unregister(httpConnection)
  val unregisterAll: UnregisterAll.type                = UnregisterAll
  val find: Find                                       = Find(akkaConnection)
  val resolve: Resolve                                 = Resolve(akkaConnection, FiniteDuration(seconds, TimeUnit.SECONDS))
  val listEntries: ListEntries.type                    = ListEntries
  val listByComponentTypeHcd: ListByComponentType      = ListByComponentType(ComponentType.HCD)
  val listByComponentTypeAssembly: ListByComponentType = ListByComponentType(ComponentType.Assembly)
  val listByHostname: ListByHostname                   = ListByHostname(hostname)
  val listByAkkaConnectionType: ListByConnectionType   = ListByConnectionType(ConnectionType.AkkaType)
  val listByHttpConnectionType: ListByConnectionType   = ListByConnectionType(ConnectionType.HttpType)
  val listByPrefix: ListByPrefix                       = ListByPrefix(prefix.toString)

  val track: Track = Track(akkaConnection)
}
