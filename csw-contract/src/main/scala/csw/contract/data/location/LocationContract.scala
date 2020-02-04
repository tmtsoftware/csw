package csw.contract.data.location

import akka.Done
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.models._
import csw.prefix.models.{Prefix, Subsystem}

object LocationContract extends LocationData with LocationServiceCodecs {
  val models: Map[String, ModelType[_]] = Map(
    name[Registration]   -> ModelType(akkaRegistration, httpRegistration, tcpRegistration),
    name[Location]       -> ModelType(akkaLocation, httpLocation, tcpLocation),
    name[TrackingEvent]  -> ModelType(locationUpdated, locationRemoved),
    name[ConnectionType] -> ModelType(ConnectionType),
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

  val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Register], name[Location], List(name[RegistrationFailed], name[OtherLocationIsRegistered])),
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

  val httpRequests: Map[String, ModelType[_]] = Map(
    name[Register]             -> ModelType(akkaRegister, httpRegister),
    name[Unregister]           -> ModelType(unregister),
    objectName(UnregisterAll)  -> ModelType(unregisterAll),
    name[Find]                 -> ModelType(find),
    name[Resolve]              -> ModelType(resolve),
    objectName(ListEntries)    -> ModelType(listEntries),
    name[ListByComponentType]  -> ModelType(listByComponentTypeHcd, listByComponentTypeAssembly),
    name[ListByConnectionType] -> ModelType(listByAkkaConnectionType, listByHttpConnectionType),
    name[ListByHostname]       -> ModelType(listByHostname),
    name[ListByPrefix]         -> ModelType(listByPrefix)
  )

  val httpContract: Contract = Contract(
    httpEndpoints,
    httpRequests
  )

  val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[Track], name[TrackingEvent])
  )

  val websocketRequests: Map[String, ModelType[_]] = Map(
    name[Track] -> ModelType(track)
  )

  val webSocketContract: Contract = Contract(
    webSocketEndpoints,
    websocketRequests
  )

  val service: Service = Service(httpContract, webSocketContract, models)
}
