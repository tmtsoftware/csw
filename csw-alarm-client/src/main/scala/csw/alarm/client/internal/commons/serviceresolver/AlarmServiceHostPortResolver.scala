package csw.alarm.client.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Provides the connection information of `Alarm Service` by using the provided host and port.
 */
class AlarmServiceHostPortResolver(host: String, port: Int) extends AlarmServiceResolver {
  override def uri(): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))
}
