package csw.contract.data.location

import akka.Done
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator.DomHelpers._
import csw.contract.generator.{Endpoint, ModelType}
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed, RegistrationListingFailed, UnregistrationFailed}
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.models._
import csw.prefix.models.{Prefix, Subsystem}
import io.bullet.borer.Dom.Element

object LocationContract extends LocationData {
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

  val requests: List[Element] = List(
    register
  )

  val httpEndpoints = Map(
    name[Register]             -> Endpoint(name[Location], List(name[RegistrationFailed])),
    name[Unregister]           -> Endpoint(name[Done], List(name[UnregistrationFailed])),
    objectName(UnregisterAll)  -> Endpoint(name[Done], List(name[UnregistrationFailed])),
    name[Find]                 -> Endpoint(arrayName[Location]),
    name[Resolve]              -> Endpoint(arrayName[Location]),
    objectName(ListEntries)    -> Endpoint(arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByComponentType]  -> Endpoint(arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByConnectionType] -> Endpoint(arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByHostname]       -> Endpoint(arrayName[Location], List(name[RegistrationListingFailed])),
    name[ListByPrefix]         -> Endpoint(arrayName[Location], List(name[RegistrationListingFailed]))
  )

  val webSocketEndpoints = Map(
    name[Track] -> Endpoint(name[TrackingEvent])
  )
}
