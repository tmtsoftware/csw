package csw.contract.data.location.models

import java.net.URI

import csw.contract.generator.models.DomHelpers.encode
import csw.contract.generator.models.ModelAdt
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions._
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models._
import csw.location.models.codecs.LocationCodecs
import csw.prefix.models.{Prefix, Subsystem}

import scala.reflect.ClassTag

object Instances extends LocationCodecs with LocationServiceCodecs {
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

  //prefix and susbsystem

  def name[T: ClassTag]: String = scala.reflect.classTag[T].runtimeClass.getSimpleName

  val models: Map[String, ModelAdt] = Map(
    name[Registration]     -> ModelAdt(akkaRegistration, httpRegistration, tcpRegistration),
    "Location"             -> ModelAdt(akkaLocation, httpLocation, tcpLocation),
    "TrackingEvent"        -> ModelAdt(locationUpdated, locationRemoved),
    "ConnectionType"       -> ModelAdt.fromEnum(ConnectionType),
    "ConnectionInfo"       -> ModelAdt(connectionInfo),
    "Connection"           -> ModelAdt(akkaConnection, httpConnection, tcpConnection),
    "ComponentId"          -> ModelAdt(ComponentId(prefix, ComponentType.HCD)),
    "ComponentType"        -> ModelAdt.fromEnum(ComponentType),
    "LocationServiceError" -> ModelAdt(registrationFailed, otherLocationIsRegistered, unregisterFailed, registrationListingFailed)
  )
}
