package csw.event.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Base trait for implementations providing connection information for Event Service
 */
trait EventServiceResolver {
  def uri(): Future[URI]
}
