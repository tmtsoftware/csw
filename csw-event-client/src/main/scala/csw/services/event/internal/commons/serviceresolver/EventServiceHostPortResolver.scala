package csw.services.event.internal.commons.serviceresolver
import java.net.URI

import scala.concurrent.Future

/**
 * Provides the connection information of `Event Service` by using the provided host and port.
 */
class EventServiceHostPortResolver(host: String, port: Int) extends EventServiceResolver {
  override def uri(): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))
}
