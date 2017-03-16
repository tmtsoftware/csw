package csw.services.location.scaladsl

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.models._

import scala.concurrent.Future

trait LocationService {

  /**
    * @param registration object
    * returns registration-result which can be used to unregister
    */
  def register(registration: Registration): Future[RegistrationResult]

  def unregister(connection: Connection): Future[Done]

  def unregisterAll(): Future[Done]

  def resolve(connection: Connection): Future[Resolved]

  def resolve(connections: Set[Connection]): Future[Set[Resolved]]

  def list: Future[List[Location]]

  def list(componentType: ComponentType): Future[List[Location]]

  def list(hostname: String): Future[List[Resolved]]

  def list(connectionType: ConnectionType): Future[List[Location]]

  def track(connection: Connection): Source[Location, KillSwitch]
}
