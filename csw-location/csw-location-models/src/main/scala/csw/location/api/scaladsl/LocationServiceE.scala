package csw.location.api.scaladsl

import akka.Done
import akka.stream.scaladsl.Source
import csw.location.api.exceptions.{RegistrationError, RegistrationListingFailed, UnregistrationFailed}
import csw.location.models._
import msocket.api.models.Subscription

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait LocationServiceE {
  def register(registration: Registration): Future[Either[RegistrationError, Location]]
  def unregister(connection: Connection): Future[Either[UnregistrationFailed, Done]]
  def unregisterAll(): Future[Either[RegistrationListingFailed, Done]]
  def find[L <: Location](connection: TypedConnection[L]): Future[Either[RegistrationListingFailed, Option[L]]]
  def resolve[L <: Location](
      connection: TypedConnection[L],
      within: FiniteDuration
  ): Future[Either[RegistrationListingFailed, Option[L]]]
  def list: Future[Either[RegistrationListingFailed, List[Location]]]
  def list(componentType: ComponentType): Future[Either[RegistrationListingFailed, List[Location]]]
  def list(hostname: String): Future[Either[RegistrationListingFailed, List[Location]]]
  def list(connectionType: ConnectionType): Future[Either[RegistrationListingFailed, List[Location]]]
  def listByPrefix(prefix: String): Future[Either[RegistrationListingFailed, List[AkkaLocation]]]
  def track(connection: Connection): Source[TrackingEvent, Subscription]
  def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription
}
