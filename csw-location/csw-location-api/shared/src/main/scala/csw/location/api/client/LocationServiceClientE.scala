package csw.location.api.client

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.github.ghik.silencer.silent
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions.{RegistrationError, RegistrationListingFailed, UnregistrationFailed}
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.scaladsl.LocationServiceE
import csw.location.models._
import msocket.api.Transport
import msocket.api.codecs.BasicCodecs
import msocket.api.models.Subscription
import msocket.impl.extensions.SourceExtensions.SourceWithSubscribe

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
class LocationServiceClientE(
    httpTransport: Transport[LocationHttpMessage],
    websocketTransport: Transport[LocationWebsocketMessage]
)(implicit @silent mat: Materializer)
    extends LocationServiceE
    with LocationServiceCodecs
    with BasicCodecs {

  override def register(registration: Registration): Future[Either[RegistrationError, Location]] =
    httpTransport.requestResponse[Either[RegistrationError, Location]](Register(registration))

  override def unregister(connection: Connection): Future[Either[UnregistrationFailed, Done]] =
    httpTransport.requestResponse[Either[UnregistrationFailed, Done]](Unregister(connection))

  override def unregisterAll(): Future[Either[RegistrationListingFailed, Done]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, Done]](UnregisterAll)

  override def find[L <: Location](connection: TypedConnection[L]): Future[Either[RegistrationListingFailed, Option[L]]] =
    httpTransport
      .requestResponse[Either[RegistrationListingFailed, Option[L]]](Find(connection.asInstanceOf[TypedConnection[Location]]))

  override def resolve[L <: Location](
      connection: TypedConnection[L],
      within: FiniteDuration
  ): Future[Either[RegistrationListingFailed, Option[L]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, Option[L]]](
      Resolve(connection.asInstanceOf[TypedConnection[Location]], within)
    )

  override def list: Future[Either[RegistrationListingFailed, List[Location]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, List[Location]]](ListEntries)

  override def list(componentType: ComponentType): Future[Either[RegistrationListingFailed, List[Location]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, List[Location]]](ListByComponentType(componentType))

  override def list(hostname: String): Future[Either[RegistrationListingFailed, List[Location]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, List[Location]]](ListByHostname(hostname))

  override def list(connectionType: ConnectionType): Future[Either[RegistrationListingFailed, List[Location]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, List[Location]]](ListByConnectionType(connectionType))

  override def listByPrefix(prefix: String): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    httpTransport.requestResponse[Either[RegistrationListingFailed, List[AkkaLocation]]](ListByPrefix(prefix))

  override def track(connection: Connection): Source[TrackingEvent, Subscription] =
    websocketTransport.requestStream[TrackingEvent](Track(connection))

  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription =
    track(connection).subscribe(callback)
}
