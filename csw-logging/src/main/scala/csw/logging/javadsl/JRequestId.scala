package csw.logging.javadsl

import csw.logging.scaladsl.RequestId

/**
 * Helper class for Java to get the handle of RequestId
 */
object JRequestId {
  val id = RequestId()
}
