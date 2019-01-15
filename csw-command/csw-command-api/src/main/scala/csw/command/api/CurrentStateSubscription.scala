package csw.command.api

trait CurrentStateSubscription {

  /**
   * Unsubscribe to the current state being published
   */
  def unsubscribe(): Unit
}
