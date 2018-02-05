package csw.services.event.scaladsl

import csw_protobuf.events.PbEvent

import scala.concurrent.Future

trait EventServiceDriver {

  def publishToChannel(key: String, data: PbEvent): Future[java.lang.Long]

}
