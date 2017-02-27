package csw.services.location.scaladsl

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait LocationService {
  def register(connection: Connection): Future[RegisterResult]

  def unregister(connection: Connection): Future[Done]

  def resolve(connection: Connection): Future[RegisterResult]

  def list(): Future[List[Connection]]

  def list(componentType: ComponentType): Future[List[Connection]]

  def list(hostname: String): Future[List[Connection]]

  def list(connectionType: ConnectionType): Future[List[Connection]]

  def track(connection: Connection): Source[ConnectionState, NotUsed]

  def untrack(connection: Connection): Future[Done]
}
