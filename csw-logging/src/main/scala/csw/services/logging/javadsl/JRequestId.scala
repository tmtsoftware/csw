package csw.services.logging.javadsl

import csw.services.logging.scaladsl.RequestId

//TODO: explain better significance why do we need this
object JRequestId {
  val id = RequestId()
}
