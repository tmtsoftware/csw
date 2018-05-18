package csw.services.event.scaladsl

/**
 * This mode is to control the rate of events received by the subscriber.
 * Parameter mode can have two values -
 * - [[SubscriptionMode.RateAdapterMode]]: This mode ensures that the subscriber receives event at the exact specified frequency.
 *
 * '''In case the publisher is faster than the subscriber:''' The subscriber receives the most recently published event at each interval.
 * Some events will be dropped because of the guarantee that exactly one event is received at the specified interval.
 *
 * '''In case the publisher is slower than the subscriber:''' The subscriber will get the last published event on each interval. The subscriber will receive duplicate events in case no element is published during that interval.
 *
 * - [[SubscriptionMode.RateLimiterMode]]: This mode ensures that events will be delivered as soon as they are generated but also ensuring that no more than one event is delivered within a given interval.
 * This mode minimizes latency, but actual interval between received events is not constant.
 *
 * '''In case the publisher is faster than the subscriber:''' The subscriber receives the element as soon as it is produced during that interval.
 * Some events will be dropped because of the guarantee that at most one event is received within an interval.
 *
 * '''In case publisher is slower than the subscriber:''' The subscriber will not receive events at some intervals.
 * The subscriber will never receive duplicate events.
 *
 * '''Note:''' If you want to minimize the latency, and do not care about receiving events at exact intervals, then use the [[SubscriptionMode.RateLimiterMode]]
 */
sealed trait SubscriptionMode

object SubscriptionMode {
  case object RateAdapterMode extends SubscriptionMode
  case object RateLimiterMode extends SubscriptionMode
}
