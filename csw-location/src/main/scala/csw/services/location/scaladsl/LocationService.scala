package csw.services.location.scaladsl

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.models._

import scala.concurrent.Future

trait LocationService {

  def register(reg: Registration): Future[RegistrationResult]

  /**
    * Unregisters the connection from the location service
    * (Note: it can take some time before the service is removed from the list: see
    * comments in registry.unregisterService())
    */
  def unregister(connection: Connection): Future[Done]
  def unregisterAll(): Future[Done]

  /**
    * Convenience method that gets the location service information for a given set of services.
    *
    * @param connections set of requested connections
    * @return a future object describing the services found
    */
  def resolve(connections: Set[Connection]): Future[Set[Resolved]]

  def resolve(connection: Connection): Future[Resolved]

  def list: Future[List[Location]]

  def list(componentType: ComponentType): Future[List[Location]]

  def list(hostname: String): Future[List[Resolved]]

  def list(connectionType: ConnectionType): Future[List[Location]]

  def track(connection: Connection): Source[Location, KillSwitch]
}
