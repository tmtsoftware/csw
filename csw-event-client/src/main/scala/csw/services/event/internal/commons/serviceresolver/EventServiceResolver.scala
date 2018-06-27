package csw.services.event.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

trait EventServiceResolver {
  def uri(): Future[URI]
}
