package csw.alarm.client.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Base trait for implementations providing connection information for Alarm Service
 */
private[client] trait AlarmServiceResolver {
  def uri(): Future[URI]
}
