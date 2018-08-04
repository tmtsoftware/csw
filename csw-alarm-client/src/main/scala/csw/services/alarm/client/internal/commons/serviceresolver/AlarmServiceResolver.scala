package csw.services.alarm.client.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Base trait for implementations providing connection information for Event Service
 */
trait AlarmServiceResolver {
  def uri(): Future[URI]
}
