/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data.location

import java.net.URI
import java.util.concurrent.TimeUnit

import org.apache.pekko.Done
import csw.location.api.exceptions._
import csw.location.api.messages.LocationRequest._
import csw.location.api.messages.LocationStreamRequest.Track
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection, TcpConnection}
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

  val pekkoConnection: PekkoConnection = PekkoConnection(componentId)
  val httpConnection: HttpConnection   = HttpConnection(componentId)
  val tcpConnection: TcpConnection     = TcpConnection(componentId)
  val connectionInfo: ConnectionInfo   = ConnectionInfo(prefix, ComponentType.HCD, ConnectionType.PekkoType)

  val metadata: Metadata = Metadata().add("key1", "value")

  val pekkoRegistration: Registration      = PekkoRegistration(pekkoConnection, uriPath, metadata)
  val httpRegistration: Registration       = HttpRegistration(httpConnection, port, pathString)
  val publicHttpRegistration: Registration = HttpRegistration(httpConnection, port, pathString, NetworkType.Outside)
  val tcpRegistration: Registration        = TcpRegistration(tcpConnection, port)

  val pekkoLocation: Location = PekkoLocation(pekkoConnection, uriPath, metadata)
  val httpLocation: Location  = HttpLocation(httpConnection, uriPath, Metadata.empty)
  val tcpLocation: Location   = TcpLocation(tcpConnection, uriPath, Metadata.empty)

  val registrationFailed: LocationServiceError        = RegistrationFailed(message)
  val otherLocationIsRegistered: LocationServiceError = OtherLocationIsRegistered(message)
  val unregisterFailed: LocationServiceError          = UnregistrationFailed(pekkoConnection)
  val registrationListingFailed: LocationServiceError = RegistrationListingFailed()

  val locationUpdated: TrackingEvent = LocationUpdated(pekkoLocation)
  val locationRemoved: TrackingEvent = LocationRemoved(pekkoConnection)

  val pekkoRegister: Register                          = Register(pekkoRegistration)
  val httpRegister: Register                           = Register(httpRegistration)
  val publicHttpRegister: Register                     = Register(publicHttpRegistration)
  val unregister: Unregister                           = Unregister(httpConnection)
  val unregisterAll: UnregisterAll.type                = UnregisterAll
  val find: Find                                       = Find(pekkoConnection)
  val resolve: Resolve                                 = Resolve(pekkoConnection, FiniteDuration(seconds, TimeUnit.SECONDS))
  val listEntries: ListEntries.type                    = ListEntries
  val listByComponentTypeHcd: ListByComponentType      = ListByComponentType(ComponentType.HCD)
  val listByComponentTypeAssembly: ListByComponentType = ListByComponentType(ComponentType.Assembly)
  val listByHostname: ListByHostname                   = ListByHostname(hostname)
  val listByPekkoConnectionType: ListByConnectionType  = ListByConnectionType(ConnectionType.PekkoType)
  val listByHttpConnectionType: ListByConnectionType   = ListByConnectionType(ConnectionType.HttpType)
  val listByPrefix: ListByPrefix                       = ListByPrefix(prefix.toString)

  val track: Track = Track(pekkoConnection)
}
