package csw.services.event.scaladsl

import csw_protobuf.events.PbEvent

import scala.concurrent.Future

trait EventServiceDriver {

  def publish(key: String, data: PbEvent): Future[Long]

}
