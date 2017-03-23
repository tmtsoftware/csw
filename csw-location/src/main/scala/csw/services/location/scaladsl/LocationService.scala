package csw.services.location.scaladsl

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.models._

import scala.concurrent.Future

trait LocationService {

  /**
    * @param location object
    * returns registration-result which can be used to unregister
    */
  def register(location: Resolved): Future[RegistrationResult]

  def unregister(connection: Connection): Future[Done]

  def unregisterAll(): Future[Done]

  def resolve(connection: Connection): Future[Option[Resolved]]

  def list: Future[List[Resolved]]

  def list(componentType: ComponentType): Future[List[Resolved]]

  def list(hostname: String): Future[List[Resolved]]

  def list(connectionType: ConnectionType): Future[List[Resolved]]

  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

}
