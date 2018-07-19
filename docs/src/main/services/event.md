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

Below example demonstrates the usage of subscribe API with actorRef.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #event-subscriber }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #event-subscriber }

In above example, `eventHandler` is the actorRef which accepts events. If you need to mutate state on receiving each event, 
then it is recommended to use this API. To use this API, you have to create an actor which takes event and then you can safely keep mutable state inside this actor.

The subscription API shown above receives events as soon as they are published. However, other APIs also support `interval` and `Subscription Mode` which help in controlling the rate of events received. This can cater multiple use cases for instance slow subscribers can receive events at their own speed rather than being overloaded with events to catch up with the publisher's speed.


There are two types of Subscription modes:

* `RateAdapterMode` which ensures that an event is received exactly at each tick of the specified interval.
* `RateLimiterMode` which ensures that events are received as they are published along with the guarantee that no more than one event is delivered within a given interval.

Read more about Subscription Mode @scaladoc[here](csw/services/event/api/scaladsl/SubscriptionMode)

### Pattern Subscription

Below example demonstrates the usage of pattern subscribe API with callback. Events with keys that match the specified pattern and belong to the given subsystem are received by the subscriber. The callback function provided is called on each event received.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #event-psubscribe }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #event-psubscribe }


@@@ warning
DO NOT include subsystem in the provided pattern. Final pattern generated will be provided pattern prepended with subsystem.
For Ex. `pSubscribe(Subsytem.WFOS, *)` will subscribe to event keys matching pattern : `wfos.*`
@@@ 

### Event Subscription
On subscription to event keys, you get a handle to `EventSubscription` which provides following APIs:

* `unsubscribe`: On un-subscribing, the event stream is destroyed and the connection created to event server while subscription is released. 

* `ready`: check if event subscription is successful or not.

You can find complete list of API's supported by `EventSubscriber` and `IEventSubscriber` with detailed description of each API here: 

* @scaladoc[EventSubscriber](csw/services/event/api/scaladsl/EventSubscriber)
* @javadoc[IEventSubscriber](csw/services/event/api/javadsl/IEventSubscriber)