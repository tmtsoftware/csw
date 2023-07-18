/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.client

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationRequest.*
import csw.location.api.messages.LocationStreamRequest.Track
import csw.location.api.messages.{LocationRequest, LocationStreamRequest}
import csw.location.api.models.*
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import msocket.api.codecs.BasicCodecs
import msocket.api.{Subscription, Transport}
import msocket.portable.Observer

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LocationServiceClient(
    httpTransport: Transport[LocationRequest],
    websocketTransport: Transport[LocationStreamRequest],
    cswVersion: CswVersion
)(implicit actorSystem: ActorSystem[_])
    extends LocationService
    with LocationServiceCodecs
    with BasicCodecs {

  import actorSystem.executionContext

  override def register(registration: Registration): Future[RegistrationResult] = {
    httpTransport
      .requestResponse[Location](Register(registration.withCswVersion(cswVersion.get)))
      .map(RegistrationResult.from(_, unregister))
  }

  override def unregister(connection: Connection): Future[Done] =
    httpTransport.requestResponse[Done](Unregister(connection))

  override def unregisterAll(): Future[Done] =
    httpTransport.requestResponse[Done](UnregisterAll)

  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] = {
    val eventualMaybeL: Future[Option[L]] =
      httpTransport.requestResponse[Option[L]](Find(connection.asInstanceOf[TypedConnection[Location]]))
    eventualMaybeL.map(validateMayBeLocation)
  }

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] = {
    val eventualMaybeL =
      httpTransport.requestResponse[Option[L]](Resolve(connection.asInstanceOf[TypedConnection[Location]], within))
    eventualMaybeL.map(validateMayBeLocation)
  }

  override def list: Future[List[Location]] = httpTransport.requestResponse[List[Location]](ListEntries)

  override def list(componentType: ComponentType): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByComponentType(componentType))

  override def list(hostname: String): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByHostname(hostname))

  override def list(connectionType: ConnectionType): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByConnectionType(connectionType))

  override def listByPrefix(prefix: String): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByPrefix(prefix))

  override def track(connection: Connection): Source[TrackingEvent, Subscription] =
    websocketTransport.requestStream[TrackingEvent](Track(connection))

  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription =
    websocketTransport.requestStream[TrackingEvent](Track(connection), Observer.create(callback))

  private def validateMayBeLocation[L <: Location](mayBeLocation: Option[L]): Option[L] = {
    mayBeLocation.map { location =>
      cswVersion.check(location.metadata, location.prefix)
      location
    }
  }
}
