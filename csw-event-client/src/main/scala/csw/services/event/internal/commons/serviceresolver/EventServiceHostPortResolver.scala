package csw.services.event.internal.commons.serviceresolver
import java.net.URI

import scala.concurrent.Future

class EventServiceHostPortResolver(host: String, port: Int) extends EventServiceResolver {
  override def uri(): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))
}
