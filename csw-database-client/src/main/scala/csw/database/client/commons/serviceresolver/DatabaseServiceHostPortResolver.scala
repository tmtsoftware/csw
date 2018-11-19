package csw.database.client.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Provides the connection information of `Database Service` by using the provided host and port
 */
class DatabaseServiceHostPortResolver(host: String, port: Int) extends DatabaseServiceResolver {
  override def uri(): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))
}
