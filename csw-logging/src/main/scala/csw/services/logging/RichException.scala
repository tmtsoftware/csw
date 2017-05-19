package csw.services.logging

import com.persist.JsonOps._

/**
 * This companion object for the RichException class. You might want to turn off
 * Throwable methods.
 */
object RichException {

  /**
   * Apply method for RichException.
   * @param richMsg the rich exception message.
   * @param cause the optional underlying causing exception.
   * @return the RichException
   */
  def apply(richMsg: RichMsg, cause: Throwable = noException) = new RichException(richMsg, cause)

  /** The unapply for matching the RichException trait.
   */
  def unapply(f: RichException): Option[(Any)] = Some((f.richMsg))

  private def stringify(j: Any): String =
    j match {
      case s: String => s
      case j: Any    => Compact(j)
    }
}

/**
 * The common parent of all rich exceptions.
 *
 * @param richMsg the rich exception message.
 * @param cause the optional underlying causing exception.
 */
class RichException(val richMsg: RichMsg, val cause: Throwable = noException)
    extends Exception(RichException.stringify(richMsg))
