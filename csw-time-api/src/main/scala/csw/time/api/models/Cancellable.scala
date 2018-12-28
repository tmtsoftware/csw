package csw.time.api.models

/**
 * Signifies something that can be cancelled.
 */
trait Cancellable {

  /**
   * Cancels this Cancellable and returns true if that was successful.
   * If this cancellable was (concurrently) cancelled already, then this method
   * will return false.
   *
   * @return whether or not the cancellable was cancelled successfully
   */
  def cancel(): Boolean
}
