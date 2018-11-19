package csw.database.client.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Base trait for implementations providing connection information for Database Service
 */
trait DatabaseServiceResolver {
  def uri(): Future[URI]
}
