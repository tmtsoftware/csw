package csw.command.api.scaladsl

trait CurrentStateSubscription {

  /**
   * Unsubscribe to the current state being published
   */
  def unsubscribe(): Unit
}
