package csw.services.logging.internal

import csw.services.logging.scaladsl.RequestId

import scala.util.Random

/**
 * Include this trait in classes you want to time.
 */
private[logging] trait Timing {

  /**
   * This object contains methods used to mark regions to be timed in the timing log.
   */
  object time {

    /**
     * Call this method to start timing a region of code for the time log.
     *
     * @param id the request id to be timed.
     * @param name each region to be timed must have a different name.
     * @return a unique token to passed to the Timing.end call.
     */
    def start(id: RequestId, name: String): String = {
      val uid = Random.nextLong().toHexString
      if (LoggingState.doTime) MessageHandler.timeStart(id, name, uid)
      uid
    }

    /**
     * Call this method to end timing a region of code for the time log.
     * The id, name, and uid must match the corresponding start.
     *
     * @param id the request id to be timed.
     * @param name each region to be timed must have a different name.
     * @param token the token returned by the Timing.start call.
     */
    def end(id: RequestId, name: String, token: String): Unit =
      if (LoggingState.doTime) MessageHandler.timeEnd(id, name, token)
  }

  /**
   * This method wraps a section of code, so that timings for it will appear in the time log.
   *
   * @param id the requestId to be timed.
   * @param name  each region to be timed must have a different name.
   * @param body  the code to be timed.
   * @tparam T  the result type of the body.
   * @return  the value of the body. If the body throws an exception than that exception will pass through the Time call.
   */
  def Time[T](id: RequestId, name: String)(body: => T): T =
    if (LoggingState.doTime) {
      val uid = Random.nextLong().toHexString
      MessageHandler.timeStart(id, name, uid)
      try {
        body
      } finally {
        MessageHandler.timeEnd(id, name, uid)
      }
    } else {
      body
    }
}
