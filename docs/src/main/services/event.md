# Event Service

Event Service provides a publish-subscribe message system supporting Event Streams made up of System Events and Observe Events.
These events are transient, historical events are not stored/persisted. 
Event Service is optimized for the high performance requirements of event streams which support multiple streams with varying rates, for ex. 100 Hz, 50 Hz etc. 
In the TMT control system Event Streams are created as an output of a calculation by one component for the input to a calculation in one or more other components.  
Event streams often consist of events that are published at a specific rate.

The Event Service provides an API that allows events to be created and published as well as allows clients to subscribe and unsubscribe to specific types of events.

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

When you create a component handlers using `csw-framework` as explained @ref:[here](./../framework/creating-components.md), 
you get a handle to `EventService` which is created by `csw-framework`

Using `EventService` you can start publishing or subscribing to events. 
`EventService` injected in component handlers via `csw-framwework` provides following features: 

* Access to `defaultPublisher`: 
Using `defaultPublisher`, you can publish event or stream of events to event server. 
In most of the cases, you should use `defaultPublisher`, because you can then pass the instance of `EvenetService` in worker actors or different places in your code 
and call `eventService.defaultPublisher` to access publisher. This way you reuse same instance of `EventPublisher` which means all events published via `defaultPublisher` goes through same tcp connection. 

* Access to `defaultSubscriber`:
Using `defaultSubscriber`, you can choose to subscribe to specific event keys. 
You can share `defaultSubscriber` same way like `defaultPublisher` by passing instance of `EventService` to different parts of your code.
Unlike `defaultPublisher`, on each subscription `defaultSubscriber.subscribe` call, new tcp connection gets created. This behavior is similar either you use `defaultSubscriber` or `makeNewSubscriber` call on `EventService`.
But whenever you create `EventSubscriber`, it creates one more tcp connection which is used to get latest event from event server (Note that this is a different connection than the one which gets created on subscription).
That means, with `defaultSubscriber`, you are sharing same connection for getting latest event and creating new for each subscribe call.

* Create new `Publisher`

* Create new `Subscriber`

In a situation where rate of publishing of events is low for ex. 1Hz, 20Hz etc. then it is good idea to use `defaultPublisher` which makes sure all events go via same connection.

For high rate event streams, you can choose to dedicate separate connection for such a event stream by creating new Publisher with `eventService.makeNewPublisher`.

## Usage of EventPublisher

Below example demonstrate the usage of publish API with event generator and onError callback.

* eventGenerator: Function responsible for generating events. You can add domain specific logic on generating new events based on certain conditions here.
* onError: Function which gets invoked if there is any failure while publishing events 

Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #event-publisher }

Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #event-publisher }

You can find complete list of API's supported by `EventPublisher` and `IEventPublisher` with detailed description of each API here: 

* @scaladoc[EventPublisher](csw/services/event/scaladsl/EventPublisher)
* @javadoc[IEventPublisher](csw/services/event/javadsl/IEventPublisher)

## Usage of EventSubscriber

Below example demonstrate the usage of subscribe API with actorRef.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #event-subscriber }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #event-subscriber }

In above example, `eventHandler` is the actorRef which accepts events. If you need to mutate state on receiving each event, 
then it is recommended to use this API. To use this API, you have to create an actor which takes event and then you can safely keep mutable state inside this actor.

You can find complete list of API's supported by `EventSubscriber` and `IEventSubscriber` with detailed description of each API here: 

* @scaladoc[EventSubscriber](csw/services/event/scaladsl/EventSubscriber)
* @javadoc[IEventSubscriber](csw/services/event/javadsl/IEventSubscriber)