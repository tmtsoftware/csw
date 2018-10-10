# Event Service

The Event Service implements the [publish/subscribe messaging paradigm](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern) where 
one component publishes an event and all components that have subscribed receive the event. In CSW, the events published are
described under the messages documentation @ref:[here](./../messages/events.md).
One advantage of this type of message system and Event Service also is that publishers and subscribers are decoupled. 
This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.
Publishers can publish regardless of whether there are subscribers, and subscribers can subscribe even if there are no publishers. 
The relationship between publishers and subscribers can be one-to-one, one-to-many, many to one, or even many-to-many. 

Event Service is optimized for the high performance requirements of events as demands with varying rates, for ex. 100 Hz, 50 Hz etc., but
can also be used with events that are published infrequently or when values change. 
In the TMT control system, events may be created as the output of a calculation by one component for the input to a calculation in 
one or more other components. Demand events often consist of events that are published at a specific rate.

The Event Service provides an API that allows events to be published and also allows 
clients to subscribe and unsubscribe to specific events and call developer code when events are received.

Event Service also stores the most recent published event for every unique event by prefix and name. This is useful for publishing
components when an event contains state information that changes. Other components needing to check the state can do so without
the overhead of subscribing. 


## Dependencies

If you already have a dependency on `csw-framework` in your `build.sbt`, then you can skip this as `csw-framework` depends on `csw-event-client`
Otherwise add below dependency in your `build.sbt`

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-event-client" % "$version$"
    ```
    @@@

## Accessing Event Service

When you create component handlers using `csw-framework` as explained @ref:[here](./../framework/creating-components.md), 
you get a handle to `EventService` which is created by `csw-framework`

Using `EventService` you can start publishing or subscribing to events. 
`EventService` is injected in component handlers via `csw-framwework` and provides the following features: 

__Access to `defaultPublisher`__: Using `defaultPublisher` in `EventService`, you can publish a single event or a stream 
of demand events to Event Service. 
In most cases, you should use `defaultPublisher`, because you can then pass the instance of `EventService` in worker actors 
or different places in your code and call `eventService.defaultPublisher` to access publisher. 
Each `EventPublisher` has its own TCP connection to the Event Service.
When you reuse the `defaultPublisher` instance of `EventPublisher`, all events published go through same TCP connection. 

__Access to `defaultSubscriber`__: Using `defaultSubscriber`, you can subscribe to specific event keys. 
You can share `defaultSubscriber` in the same way as `defaultPublisher` by passing an instance of `EventService` to different parts of your code.
Unlike `defaultPublisher`, each subscription with `defaultSubscriber.subscribe` creates a new TCP connection for just that subscription. 
This behavior is the same whether you use `defaultSubscriber` or `makeNewSubscriber` call on `EventService`.

Each `EventSubscriber` also has one TCP connection that is used to provide the latest event from the event server when
subscribing and also for the explicit `get` calls.  That means, with `defaultSubscriber`, you are sharing same connection 
for getting latest events and creating a new connection for each subscribe call.  The underlying event server can handle
many connections, but it is good to understand how connections are used and reused.

__Creating a new `Publisher` or `Subscriber`__:
The `makeNewPublisher` API of Event Service can be used to create a new publisher which would internally create a new TCP connection to the Event Store.
One of the use cases of this API could be to publish high frequency event streams in order to dedicate a separate connection to demanding streams without affecting the performance of all other low frequency (for ex. 1Hz, 20Hz etc.) event streams.

However, `makeNewSubscriber` API does not really have any specific use cases. Both `defaultSubscriber` and `makeNewSubscriber` APIs behave almost similar since the `subscribe` API of EventService itself creates a new connection for every subscription. 
Prefer using `defaultSubscriber` over `makeNewSubscriber`.

## Usage of EventPublisher

Below examples demonstrate the usage of multiple variations of publish API.

### For Single Event

This is the simplest API to publish a single event. It returns a Future which will complete successfully if the event is published or 
fail immediately with a @scaladoc[PublishFailure](csw/event/api/exceptions/PublishFailure) exception if the component cannot
publish the event.

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/csw/event/EventPublishExamples.scala) { #single-event }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/csw/event/JEventPublishExamples.java) { #single-event }

### With Generator

A generator is useful when component code needs to publish events with a specific frequency.
The following example demonstrates the usage of publish API with event generator which will publish one event 
at each `interval`. `eventGenerator` is a function responsible for generating events. It can hold domain specific logic 
of generating new events based on certain conditions.

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/csw/event/EventPublishExamples.scala) { #event-generator }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/csw/event/JEventPublishExamples.java) { #event-generator }

### With Event Stream

In order to publish a continuous stream of events, this stream-based API can also be used. 
If an infinite stream is provided, shutdown of the stream needs to be taken care by the users.
(Note that streams discussed here are an Akka feature that is supported in event publisher and subscriber APIs. 
See [Akka stream documentation.](https://doc.akka.io/docs/akka/current/stream/index.html?language=scala))

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/csw/event/EventPublishExamples.scala) { #with-source }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/csw/event/JEventPublishExamples.java) { #with-source }

This API also demonstrates the usage of onError callback which can be used to be alerted to and handle events that failed while being published. 
The `eventGenerator` API showed just above also demonstrates the use of the `onError` callback.


You can find complete list of APIs supported by `EventPublisher` and `IEventPublisher` with detailed description of each API here: 

* @scaladoc[EventPublisher](csw/event/api/scaladsl/EventPublisher)
* @javadoc[IEventPublisher](csw/event/api/javadsl/IEventPublisher)

## Usage of EventSubscriber

The EventSubscriber API has several options available that are useful in different situations.
Examples below demonstrate the usage of multiple variations available in the subscribe API.

### With Callback

The example shown below takes a set of event keys to subscribe to and a callback function which will be called on each 
event received by the event stream. This is the simplest and most commonly used API. The example below uses an inline
function, but that is not necessary. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #with-callback }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #with-callback }

### With Asynchronous Callback

The above example will run into concurrency issues if the callback has an shared state or asynchronous behavior. 
To avoid that use the following API which will give the guarantee of ordered execution of these asynchronous callbacks.
In this case, no further processing occurs until the Future completes.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #with-async-callback }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #with-async-callback }

### With ActorRef

If there is a need to mutate state on receiving each event, then it is recommended to use this API and send a message to an actor. 
To use this API, you have to create an actor which takes event and then you can safely keep mutable state inside this actor. 
In the example shown below, `eventHandler` is the actorRef which accepts events. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #with-actor-ref }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #with-actor-ref }


### Receive Event Stream

This API takes a set of Event keys to subscribe to and returns a 
Source of events (see [Akka stream documentation](https://doc.akka.io/docs/akka/current/stream/index.html?language=scala)). 
This API gives more control to the user to customize behavior of an event stream.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #with-source }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #with-source }

### Controlling Subscription Rate

In all the examples shown above, events are received by the subscriber as soon as they are published. 
There will be scenarios where you would like to control the rate of events received by your code. 
For instance, slow subscribers can receive events at their own specified speed rather than being overloaded 
with events to catch up with the publisher's speed. 

All the APIs in EventSubscriber can be provided with `interval` and `SubscriptionMode` 
to control the subscription rate. Following example demonstrates this with the subscribeCallback API. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #with-subscription-mode }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #with-subscription-mode }
 

There are two types of Subscription modes:

* `RateAdapterMode` which ensures that an event is received exactly at each tick of the specified interval.
* `RateLimiterMode` which ensures that events are received as they are published along with the guarantee that 
no more than one event is delivered within a given interval.

Read more about Subscription Mode @scaladoc[here](csw/event/api/scaladsl/SubscriptionMode)

### Pattern Subscription

The following example demonstrates the usage of pattern subscribe API with callback. Events with keys that match the specified pattern 
and belong to the given subsystem are received by the subscriber. 
The callback function provided is called on each event received.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/csw/event/EventSubscribeExamples.scala) { #psubscribe }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/csw/event/JEventSubscribeExamples.java) { #psubscribe }


@@@ warning
DO NOT include subsystem in the provided pattern. Final pattern generated will be provided pattern prepended with subsystem.
For Ex. `pSubscribe(Subsytem.WFOS, *)` will subscribe to event keys matching pattern : `wfos.*`
@@@ 

@@@ warning
The pattern-based subscribe API is provided because it is useful in testing, but *should not be used in production code*.
The use of certain patterns and many pattern-based subscriptions can impact the overall-performance of the Event Service.
@@@ 

### Event Subscription
On subscription to event keys, you receive an @scaladoc[EventSubscription](csw/event/api/scaladsl/EventSubscription) 
which provides following APIs:

* `unsubscribe`: On un-subscribing, the event stream is destroyed and the connection created to event server while subscription is released. 

* `ready`: check if event subscription is successful or not. 

You can find complete list of API's supported by `EventSubscriber` and `IEventSubscriber` with detailed description 
of each API here: 

* @scaladoc[EventSubscriber](csw/event/api/scaladsl/EventSubscriber)
* @javadoc[IEventSubscriber](csw/event/api/javadsl/IEventSubscriber)

## Create Event Service
If you are not using csw-framework, you can create @scaladoc[EventService](csw/event/api/scaladsl/EventService) 
using @scaladoc[EventServiceFactory](csw/event/EventServiceFactory).

Scala
:   @@snip [EventServiceCreationExamples.scala](../../../../examples/src/main/scala/csw/event/EventServiceCreationExamples.scala) { #default-event-service }

Java
:   @@snip [JEventServiceCreationExamples.java](../../../../examples/src/main/java/csw/event/JEventServiceCreationExamples.java) { #default-event-service }

The provided implementation of Event Service is backed up by Redis. The above example demonstrates creation of Event Service 
with default Redis client options. 
You can optionally supply a RedisClient to the EventStore from outside which allows 
you to customize the behaviour of RedisClient used by Event Service, which in most often be required in test scope only. 

RedisClient is an expensive resource. Reuse this instance as much as possible.

Note that it is the responsibility of consumer of this API to shutdown Redis Client when it is no longer in use.

Scala
:   @@snip [EventServiceCreationExamples.scala](../../../../examples/src/main/scala/csw/event/EventServiceCreationExamples.scala) { #redis-event-service }

Java
:   @@snip [JEventServiceCreationExamples.java](../../../../examples/src/main/java/csw/event/JEventServiceCreationExamples.java) { #redis-event-service }
