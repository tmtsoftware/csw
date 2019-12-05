package csw.location.api.client

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models._
import msocket.api.{Subscription, Transport}
import msocket.api.codecs.BasicCodecs
import portable.akka.extensions.PortableAkka.SourceWithSubscribe

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LocationServiceClient(
    httpTransport: Transport[LocationHttpMessage],
    websocketTransport: Transport[LocationWebsocketMessage]
)(implicit actorSystem: ActorSystem[_])
    extends LocationService
    with LocationServiceCodecs
    with BasicCodecs {

  import actorSystem.executionContext

  override def register(registration: Registration): Future[RegistrationResult] =
    httpTransport.requestResponse[Location](Register(registration)).map(RegistrationResult.from(_, unregister))

  override def unregister(connection: Connection): Future[Done] =
    httpTransport.requestResponse[Done](Unregister(connection))

  override def unregisterAll(): Future[Done] =
    httpTransport.requestResponse[Done](UnregisterAll)

  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] =
    httpTransport.requestResponse[Option[L]](Find(connection.asInstanceOf[TypedConnection[Location]]))

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] =
    httpTransport.requestResponse[Option[L]](Resolve(connection.asInstanceOf[TypedConnection[Location]], within))

  override def list: Future[List[Location]] = httpTransport.requestResponse[List[Location]](ListEntries)

  override def list(componentType: ComponentType): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByComponentType(componentType))

  override def list(hostname: String): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByHostname(hostname))

  override def list(connectionType: ConnectionType): Future[List[Location]] =
    httpTransport.requestResponse[List[Location]](ListByConnectionType(connectionType))

  override def listByPrefix(prefix: String): Future[List[AkkaLocation]] =
    httpTransport.requestResponse[List[AkkaLocation]](ListByPrefix(prefix))

  override def track(connection: Connection): Source[TrackingEvent, Subscription] =
    websocketTransport.requestStream[TrackingEvent](Track(connection))

  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription =
    track(connection).subscribe(callback)
}
