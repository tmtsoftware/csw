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
  def register(location: Location): Future[RegistrationResult]

  def unregister(connection: Connection): Future[Done]

  def unregisterAll(): Future[Done]

  def resolve(connection: Connection): Future[Option[Location]]

  def list: Future[List[Location]]

  def list(componentType: ComponentType): Future[List[Location]]

  def list(hostname: String): Future[List[Location]]

  def list(connectionType: ConnectionType): Future[List[Location]]

  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

}
