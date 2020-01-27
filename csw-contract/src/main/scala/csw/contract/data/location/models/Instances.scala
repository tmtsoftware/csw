package csw.contract.data.location.models

import java.net.URI

import csw.contract.generator.models.DomHelpers.encode
import csw.contract.generator.models.ModelAdt
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions._
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models.ConnectionType.{AkkaType, HttpType, TcpType}
import csw.location.models._
import csw.location.models.codecs.LocationCodecs
import csw.prefix.models.{Prefix, Subsystem}

object Instances extends LocationCodecs with LocationServiceCodecs {
  private val port             = 8080
  private val prefix: Prefix   = Prefix(Subsystem.CSW, "componentName")
  val componentId: ComponentId = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)

  val akkaType: ConnectionType = AkkaType
  val httpType: ConnectionType = HttpType
  val tcpType: ConnectionType  = TcpType

  val akkaConnection: AkkaConnection = AkkaConnection(componentId)
  val httpConnection: HttpConnection = HttpConnection(componentId)
  val tcpConnection: TcpConnection   = TcpConnection(componentId)
  val connectionInfo: ConnectionInfo = ConnectionInfo(prefix, ComponentType.HCD, akkaType)

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

  val models: Map[String, ModelAdt] = Map(
    "Registration" -> ModelAdt(
      List(akkaRegistration, httpRegistration, tcpRegistration)
    ),
    "Location" -> ModelAdt(
      List(akkaLocation, httpLocation, tcpLocation)
    ),
    "TrackingEvent" -> ModelAdt(
      List(locationUpdated, locationRemoved)
    ),
    "ConnectionType" -> ModelAdt(List(akkaType, httpType, tcpType)),
    "ConnectionInfo" -> ModelAdt(List(connectionInfo)),
    "Connection"     -> ModelAdt(List(akkaConnection, httpConnection, tcpConnection)),
    "ComponentId"    -> ModelAdt(List(ComponentId(prefix, ComponentType.HCD))),
    "ComponentType" -> ModelAdt(
      List(
        ComponentType.HCD.name,
        ComponentType.Assembly.name,
        ComponentType.Container.name,
        ComponentType.Sequencer.name,
        ComponentType.SequenceComponent.name,
        ComponentType.Service.name,
        ComponentType.Machine.name
      )
    ),
    "LocationServiceError" -> ModelAdt(
      List(
        registrationFailed,
        otherLocationIsRegistered,
        unregisterFailed,
        registrationListingFailed
      )
    )
  )
}
