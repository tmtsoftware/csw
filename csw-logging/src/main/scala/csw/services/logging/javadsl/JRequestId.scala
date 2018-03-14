package csw.services.logging.javadsl

import csw.services.logging.scaladsl.RequestId

/**
 * Helper class for Java to get the handle of RequestId
 */
object JRequestId {
  val id = RequestId()
}
