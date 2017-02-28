package csw.services.location.scaladsl

import akka.Done

import scala.concurrent.Future

trait LocationService {
  /**
    * Registers a component connection with the location sevice.
    * The component will automatically be unregistered when the vm exists or when
    * unregister() is called on the result of this method.
    *
    * @param reg    component registration information
    * @return a future result that completes when the registration has completed and can be used to unregister later
    */
  def register(reg: Registration): RegistrationResult

  /**
    * Unregisters the connection from the location service
    * (Note: it can take some time before the service is removed from the list: see
    * comments in registry.unregisterService())
    */
  def unregisterConnection(connection: Connection): Future[Done]

  /**
    * Convenience method that gets the location service information for a given set of services.
    *
    * @param connections set of requested connections
    * @return a future object describing the services found
    */
  def resolve(connections: Set[Connection]): Future[Set[Location]]

  def list(): Future[List[Location]]

  def list(componentType: ComponentType): Future[List[Location]]

  def list(hostname: String): Future[List[Location]]

  def list(connectionType: ConnectionType): Future[List[Location]]

  def track(connection: Connection): TrackingResult
}
