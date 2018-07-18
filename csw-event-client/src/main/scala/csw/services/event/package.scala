package csw.services

/**
 * == Event Service ==
 *
 * This module implements an Event Service responsible for publishing an [[csw.messages.events.Event]] or subscribing to an [[csw.messages.events.Event]].
 * An event can be published and subscribers can receive the events.
 *
 * === Example: Event Service ===
 *
 * {{{
 *
 *      val eventServiceFactory          = new RedisEventServiceFactory()
 *      val eventService: EventService   = eventServiceFactory.make(locationService)
 *
 * }}}
 *
 * Using above code, you can create instance of [[csw.services.event.api.scaladsl.EventService]]. EventService is nothing but the factory to create publishers and subscribers.
 *
 * You can choose to use defaultPublisher in case you want to share same connection for publishing different events.
 *
 * === Example: Event Publisher ===
 *
 * {{{
 *
 *      val event = SystemEvent(prefix, EventName("filter_wheel"))
 *
 *      async {
 *        val publisher    = await(eventService.defaultPublisher)
 *        publisher.publish(event)
 *      }
 *
 * }}}
 *
 * === Example: Event Subscriber (subscribe and get API) ===
 *
 * The EventSubscriber provides various API's to subscribe to [[csw.messages.events.EventKey]].
 * One of such a subscribe method takes an ActorRef of an arbitrary actor or a callback function to be called
 * when an event matching the given [[csw.messages.events.EventKey]] is received. In the example below, we only provide the callback
 * argument value. You could also provide an ActorRef of some actor that should receive the Event message.
 *
 * {{{
 *
 *      def callback(ev: Event): Unit = {
 *        // ...
 *      }
 *
 *      async {
 *        val subscriber    = await(eventService.defaultSubscriber)
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
 */
package object event {}
