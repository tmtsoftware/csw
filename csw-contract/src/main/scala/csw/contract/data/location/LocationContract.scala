package csw.contract.data.location

import akka.Done
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator.DomHelpers._
import csw.contract.generator.{ContractCodecs, Endpoint, ModelType}
import csw.location.api.exceptions.{LocationServiceError, RegistrationFailed, RegistrationListingFailed, UnregistrationFailed}
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.models._
import csw.prefix.models.{Prefix, Subsystem}
import io.bullet.borer.Dom.Element

object LocationContract extends LocationData with ContractCodecs {
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

  val httpRequests: List[LocationHttpMessage] = List(
    register,
    unregister,
    unregisterAll,
    find,
    resolve,
    listEntries,
    listByComponentType,
    listByConnectionType,
    listByHostname,
    listByPrefix
  )

  val websocketRequests: List[LocationWebsocketMessage] = List(
    track
  )

  val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Register], name[Location], List(name[RegistrationFailed])),
    Endpoint(name[Unregister], name[Done], List(name[UnregistrationFailed])),
    Endpoint(objectName(UnregisterAll), name[Done], List(name[UnregistrationFailed])),
    Endpoint(name[Find], arrayName[Location]),
    Endpoint(name[Resolve], arrayName[Location]),
    Endpoint(objectName(ListEntries), arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByComponentType], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByConnectionType], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByHostname], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByPrefix], arrayName[Location], List(name[RegistrationListingFailed]))
  )

  val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[Track], name[TrackingEvent])
  )

  val http: Map[String, Element] = Map(
    "endpoints" -> httpEndpoints,
    "requests"  -> httpRequests
  )

  val webSockets: Map[String, Element] = Map(
    "endpoints" -> webSocketEndpoints,
    "requests"  -> websocketRequests
  )
}
