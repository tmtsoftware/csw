package csw.services.event.scaladsl

sealed trait SubscriptionMode

object SubscriptionMode {
  case object RateLimiterMode extends SubscriptionMode
  case object RateAdapterMode extends SubscriptionMode
}
