package csw.services

/**
 * == Event Service ==
 *
 * The Event Service provides the capability to publish and subscribe one or more [[csw.messages.events.Event]].
 * An event is published on a [[csw.messages.events.EventKey]]. The event key is composed of a [[csw.messages.params.models.Prefix]]
 * depicting the source and an [[csw.messages.events.EventName]].
 * The subscriber can subscribe to the [[csw.messages.events.EventKey]] to receive all events published on the Key.
 *
 * The Event Service provides access to [[csw.services.event.scaladsl.EventPublisher]] and [[csw.services.event.scaladsl.EventSubscriber]]
 *
 * == Event Publisher ==
 * Event Publisher provides asynchronous APIs to publish one or more Events
 *
 * == Event Subscriber ==
 * Event Subscriber provides asynchronous APIs to subscribe to one or more Event Keys.
 * It also provides APIs to subscribe using a pattern.
 * In addition, there are APIs to `get` latest events for one ore more Event Keys without making a subscription
 *
 * == Event Service Implementation ==
 *
 * `csw-prod` provides two implementations for Event Service which can be accessed through `csw-event-client` :
 * 1. RedisEventService - Redis as event store and publisher/subscriber functionality
 * 2. KafkaEventService - Kafka as event store and publisher/subscriber functionality
 *
 *
 * Complete guide of usage of different API's provided by EventService is available at:
 * https://tmtsoftware.github.io/csw-prod/services/event.html
 */
package object event {}
