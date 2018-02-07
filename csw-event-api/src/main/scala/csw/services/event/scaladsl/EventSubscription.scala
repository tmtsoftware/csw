package csw.services.event.scaladsl

trait EventSubscription {
  def unsubscribe(): Unit
}
