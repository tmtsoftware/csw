package csw.services

import com.persist.JsonOps._

/**
 * The package for the logging API.
 */
package object logging {

  /**
   * The type for rich messages.
   * This can be a String or Map[String,String]
   * See the project README file for other options.
   *
   */
  type RichMsg = Any

  /**
   * Marker to indicate no exception is present
   */
  val noException = new Exception("No Exception")

  /**
   * Convert a rich message to a printable string.
   * @param m  the rich message.
   * @return  a string that shows the message contents.
   */
  def richToString(m: RichMsg): String =
    m match {
      case s: String => s
      case x         => Compact(x, safe = true)
    }
}
