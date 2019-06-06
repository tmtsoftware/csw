package csw.command.api

trait CommandUpdateSubscription {

  /**
   * Unsubscribe to the command update state being published
   */
  def unsubscribe(): Unit
}
