package csw.command.api

/**
 * The handle to the subscription created for the current state published by components.
 */
trait CurrentStateSubscription {

  /**
   * Unsubscribe to the current state being published
   */
  def unsubscribe(): Unit
}
