package csw.event

/**
 * == Event Service ==
 *
 * This module implements an Event Service responsible for publishing an [[csw.params.events.Event]] or subscribing to an [[csw.params.events.Event]].
 * An event is published on a [[csw.params.events.EventKey]]. The event key is composed of a [[csw.params.core.models.Prefix]] depicting the source and an [[csw.params.events.EventName]].
 * The subscriber can subscribe to the [[csw.params.events.EventKey]] to receive all events published on the Key.
 *
 * === Example: Event Service ===
 *
 * {{{
 *
 *      val eventServiceFactory          = new EventServiceFactory()
 *      val eventService: EventService   = eventServiceFactory.make(locationService)
 *
 * }}}
 *
 * Using above code, you can create instance of [[csw.event.api.scaladsl.EventService]]. EventService is the factory to create publishers and subscribers.
 *
 * You can choose to use defaultPublisher in case you want to share same connection for publishing different events.
 *
 * === Example: Event Publisher ===
 *
 * Event Publisher provides asynchronous APIs to publish one or more Events
 *
 * {{{
 *
 *      val event = SystemEvent(prefix, EventName("filter_wheel"))
 *
 *      val publisher    = eventService.defaultPublisher
 *      publisher.publish(event)
 *
 * }}}
 *
 * === Example: Event Subscriber (subscribe and get API) ===
 *
 * Event Subscriber provides asynchronous APIs to subscribe to one or more Event Keys.
 * It also provides APIs to subscribe using a pattern.
 * In addition, there are APIs to `get` latest events for one ore more Event Keys without making a subscription
 *
 * The EventSubscriber provides various API's to subscribe to [[csw.params.events.EventKey]].
 * One of such a subscribe method takes an ActorRef of an arbitrary actor or a callback function to be called
 * when an event matching the given [[csw.params.events.EventKey]] is received. In the example below, we only provide the callback
 * argument value. You could also provide an ActorRef of some actor that should receive the Event message.
 *
 * {{{
 *
 *      def callback(ev: Event): Unit = {
 *        // ...
 *      }
 *
 *      async {
 *        val subscriber    = eventService.defaultSubscriber
 *        subscriber.subscribeCallback(Set(EventKey(prefix, EventName("filter_wheel"))), callback)
 *
 *        val event = await(subscriber.get(event.eventKey))
 *
 *        event match {
 *          case e: ObserveEvent => assert(e.prefix == expectedPrefix)
 *          case _               => fail("Expected ObserveEvent")
 *        }
 *     }
 *
 * }}}
 *
 * `csw-prod` provides two implementations for Event Service which can be accessed through `csw-event-client` :
 * 1. [[csw.event.client.internal.redis.RedisEventService]] - Redis as event store and publisher/subscriber functionality
 * 2. [[csw.event.client.internal.kafka.KafkaEventService]] - Kafka as event store and publisher/subscriber functionality
 *
 *
 * Complete guide of usage of different API's provided by EventService is available at:
 * https://tmtsoftware.github.io/csw-prod/services/event.html
 *
 */
package object client {}
