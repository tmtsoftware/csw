package csw.location.api.client

import akka.Done
import akka.stream.scaladsl.Source
import csw.location.api.extensions.EitherExtensions.RichEither
import csw.location.api.scaladsl.{LocationService, LocationServiceE, RegistrationResult}
import csw.location.models._
import msocket.api.models.Subscription

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class LocationServiceClient(
    client: LocationServiceE
)(implicit ec: ExecutionContext)
    extends LocationService {

  override def register(registration: Registration): Future[RegistrationResult] =
    client.register(registration).toFuture.map(RegistrationResult.from(_, unregister))
  override def unregister(connection: Connection): Future[Done]                       = client.unregister(connection).toFuture
  override def unregisterAll(): Future[Done]                                          = client.unregisterAll().toFuture
  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] = client.find(connection).toFuture
  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] =
    client.resolve(connection, within).toFuture
  override def list: Future[List[Location]]                                       = client.list.toFuture
  override def list(componentType: ComponentType): Future[List[Location]]         = client.list(componentType).toFuture
  override def list(hostname: String): Future[List[Location]]                     = client.list(hostname).toFuture
  override def list(connectionType: ConnectionType): Future[List[Location]]       = client.list(connectionType).toFuture
  override def listByPrefix(prefix: String): Future[List[AkkaLocation]]           = client.listByPrefix(prefix).toFuture
  override def track(connection: Connection): Source[TrackingEvent, Subscription] = client.track(connection)
  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription =
    client.subscribe(connection, callback)

  override def locationServiceE: LocationServiceE = client
}
