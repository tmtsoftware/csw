# Event Service

The Event Service implements the [publish/subscribe messaging paradigm](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern) where one component publishes an event and all components that have subscribed receive the event.
The advantage of this type of message system is that publishers and subscribers are decoupled. This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.
Publishers can publish regardless of whether there are subscribers, and subscribers can subscribe even if there are no publishers. The relationship between publishers and subscribers can be one-to-one, one-to-many, many to one, or even many-to-many. 

Event Service is optimized for the high performance requirements of event streams which support multiple streams with varying rates, for ex. 100 Hz, 50 Hz etc. In the TMT control system event streams are created as an output of a calculation by one component for the input to a calculation in one or more other components. Event streams often consist of events that are published at a specific rate. These events are transient, historical events are not stored/persisted. 

The Event Service provides an API that allows @ref:[Event](./../messages/events.md)s to be created and published as well as allows clients to subscribe and unsubscribe to specific types of events.

## Dependencies

If you already have a dependency on `csw-framework` in your `build.sbt`, then you can skip this as `csw-framework` depends on `csw-event-client`
Otherwise add below dependency in your `build.sbt`

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-event-client" % "$version$"
    ```
    @@@

## Accessing Event Service

When you create component handlers using `csw-framework` as explained @ref:[here](./../framework/creating-components.md), 
you get a handle to `EventService` which is created by `csw-framework`

Using `EventService` you can start publishing or subscribing to events. 
`EventService` injected in component handlers via `csw-framwework` provides following features: 

* Access to `defaultPublisher`: 
Using `defaultPublisher`, you can publish event or stream of events to event server. 
In most of the cases, you should use `defaultPublisher`, because you can then pass the instance of `EventService` in worker actors or different places in your code and call `eventService.defaultPublisher` to access publisher. This way you reuse same instance of `EventPublisher` which means all events published via `defaultPublisher` go through same tcp connection. 

* Access to `defaultSubscriber`:
Using `defaultSubscriber`, you can subscribe to specific event keys. 
You can share `defaultSubscriber` same way like `defaultPublisher` by passing instance of `EventService` to different parts of your code.
Unlike `defaultPublisher`, each subscription `defaultSubscriber.subscribe` call creates a new tcp connection. This behavior is similar either you use `defaultSubscriber` or `makeNewSubscriber` call on `EventService`.
But whenever you create a new `EventSubscriber`, it creates one more tcp connection which is used to get latest event from event server (Note that this is a different connection than the one which gets created on subscription).
That means, with `defaultSubscriber`, you are sharing same connection for getting latest events and creating a new connection for each subscribe call.

* Create new `Publisher` or `Subscriber`: Whenever a new publisher or subscriber is created, location of event service is resolved. So, in case your event server goes down, and you wish to re-resolve the event server location, you would make a call to `makeNewPublisher` or `makeNewSubscriber` instead of using the `defaultPublisher` and `defaultSubscriber`. Apart from the above mentioned scenario, one can choose to create a new publisher in case of high frequency event streams to dedicate a separate connection to demanding streams without affecting the performance of all other low frequency (for ex. 1Hz, 20Hz etc.) event streams.

## Usage of EventPublisher

Below example demonstrates the usage of publish API with event generator to publish events generated at specified interval and call onError callback to log events that were failed to be published.

* eventGenerator: Function responsible for generating events. You can add domain specific logic of generating new events based on certain conditions.
* onError: Function which gets invoked on events which were failed to be published.

Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #event-publisher }

Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #event-publisher }

You can find complete list of APIs supported by `EventPublisher` and `IEventPublisher` with detailed description of each API here: 

* @scaladoc[EventPublisher](csw/services/event/api/scaladsl/EventPublisher)
* @javadoc[IEventPublisher](csw/services/event/api/javadsl/IEventPublisher)

## Usage of EventSubscriber

Below examples demonstrate the usage of multiple variations of subscribe API.

### With Callback

The example shown below takes a set of event keys to subscribe to and a callback function which will be called on each event received by the event stream. This is the simplest and most commonly used API. 

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #with-callback }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #with-callback }

### With Asynchronous Callback

The above example will run into concurrency issues, if the callback has a asynchronous behavior. To avoid that use the following API which will give the guarantee of ordered execution of these asynchronous callbacks.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #with-async-callback }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #with-async-callback }

### With ActorRef

If there is a need to mutate state on receiving each event, then it is recommended to use this API. To use this API, you have to create an actor which takes event and then you can safely keep mutable state inside this actor. In the example shown below, `eventHandler` is the actorRef which accepts events. 

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #with-actor-ref }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #with-actor-ref }


### Receive Event Stream

This API takes a set of Event keys to subscribe to and returns a Source of events. This API gives more control to the user to customize behavior of the event stream.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #with-source }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #with-source }

### Controlling Subscription Rate

In all the examples shown above, events are received by the subscriber as soon as they are published. There will be scenarios where you would like to control the rate of events received. For instance, slow subscribers can receive events at their own specified speed rather than being overloaded with events to catch up with the publisher's speed. 

All the APIs in EventSubscriber can be provided with `interval` and `SubscriptionMode` to control the subscription rate. Following example demonstrates this with the subscribeCallback API. 

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #with-subscription-mode }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #with-subscription-mode }
 

There are two types of Subscription modes:

* `RateAdapterMode` which ensures that an event is received exactly at each tick of the specified interval.
* `RateLimiterMode` which ensures that events are received as they are published along with the guarantee that no more than one event is delivered within a given interval.

Read more about Subscription Mode @scaladoc[here](csw/services/event/api/scaladsl/SubscriptionMode)

### Pattern Subscription

Below example demonstrates the usage of pattern subscribe API with callback. Events with keys that match the specified pattern and belong to the given subsystem are received by the subscriber. The callback function provided is called on each event received.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/SubscribeExamples.scala) { #psubscribe }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JSubscribeExamples.java) { #psubscribe }


@@@ warning
DO NOT include subsystem in the provided pattern. Final pattern generated will be provided pattern prepended with subsystem.
For Ex. `pSubscribe(Subsytem.WFOS, *)` will subscribe to event keys matching pattern : `wfos.*`
@@@ 

### Event Subscription
On subscription to event keys, you get a handle to @scaladoc[EventSubscription](csw/services/event/api/scaladsl/EventSubscription) which provides following APIs:

* `unsubscribe`: On un-subscribing, the event stream is destroyed and the connection created to event server while subscription is released. 

* `ready`: check if event subscription is successful or not.

You can find complete list of API's supported by `EventSubscriber` and `IEventSubscriber` with detailed description of each API here: 

* @scaladoc[EventSubscriber](csw/services/event/api/scaladsl/EventSubscriber)
* @javadoc[IEventSubscriber](csw/services/event/api/javadsl/IEventSubscriber)