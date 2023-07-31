/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data.location

import org.apache.pekko.Done
import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers.*
import csw.contract.generator.*
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions.*
import csw.location.api.messages.LocationRequest.*
import csw.location.api.messages.LocationStreamRequest.Track
import csw.location.api.messages.{LocationRequest, LocationStreamRequest}
import csw.location.api.models.*
import csw.prefix.models.Subsystem

object LocationContract extends LocationData with LocationServiceCodecs {
  private val models: ModelSet = ModelSet.models(
    ModelType(pekkoRegistration, httpRegistration, publicHttpRegistration, tcpRegistration),
    ModelType(pekkoLocation, httpLocation, tcpLocation),
    ModelType(locationUpdated, locationRemoved),
    ModelType(ConnectionType),
    ModelType[Connection](pekkoConnection, httpConnection, tcpConnection),
    ModelType(ComponentId(prefix, ComponentType.HCD)),
    ModelType(ComponentType),
    ModelType(
      registrationFailed,
      otherLocationIsRegistered,
      unregisterFailed,
      registrationListingFailed
    ),
    ModelType(Subsystem),
    ModelType(prefix)
  )

  private val httpEndpoints: List[Endpoint] = List(
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

  private val httpRequests = new RequestSet[LocationRequest] {
    requestType(pekkoRegister, httpRegister, publicHttpRegister)
    requestType(unregister)
    requestType(unregisterAll)
    requestType(find)
    requestType(resolve)
    requestType(listEntries)
    requestType(listByComponentTypeHcd, listByComponentTypeAssembly)
    requestType(listByPekkoConnectionType, listByHttpConnectionType)
    requestType(listByHostname)
    requestType(listByPrefix)
  }

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[Track], name[TrackingEvent])
  )

  private val websocketRequests = new RequestSet[LocationStreamRequest] {
    requestType(track)
  }

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("location-service/README.md"))

  val service: Service = Service(
    Contract(httpEndpoints, httpRequests),
    Contract(webSocketEndpoints, websocketRequests),
    models,
    readme
  )
}
