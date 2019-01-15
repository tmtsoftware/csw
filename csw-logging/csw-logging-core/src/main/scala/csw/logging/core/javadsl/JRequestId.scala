package csw.logging.core.javadsl

import csw.logging.core.scaladsl.RequestId

/**
 * Helper class for Java to get the handle of RequestId
 */
object JRequestId {
  val id = RequestId()
}
