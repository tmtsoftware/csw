# Event Service

## Introduction

Event Service is a [PubSub](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern) Service which allows publishing and subscription of 
@ref:[Events](./../../messages/events.md) based on Event Key which is a combination of Prefix and Event Name. 
Event Service is optimized for the high performance requirements of events as demands with varying rates, for ex. 100 Hz, 50 Hz etc., but
can also be used with events that are published infrequently or when values change.
The end-to-end latency of events assured by Event service is 5 milliseconds. It also ensures ordered delivery of events with no event loss within performance specification. 

In the TMT control system, events may be created as the output of a calculation by one component for the input to a calculation in 
one or more other components. Demand events often consist of events that are published at a specific rate.

## Technology Choices

There were two good candidates for the backend of Event Service - [Apache Kafka](https://kafka.apache.org/) and [Redis](https://redis.io/). 
The Event Service API is implemented with both the backends and a performance testing was done to select one particular backend 
which would cater to our low latency requirements. Results of the performance tests could be found 
[here](https://tmt-project.atlassian.net/wiki/spaces/DEOPSCSW/pages/191791210/Event+Service+Raw+Performance+Results+Results+May+Change).
Redis seemed to be a good choice for the backend as it turned out to be better at providing low latency 
unlike Kafka which is more suited for high throughput systems.
Hence you can see 2 implementations of the API in the Event Service client. The code is structured in a way that it is easy to switch the implementations.

## Implementation Details

@scaladoc[EventServiceFactory](csw.event.client.EventServiceFactory) in `csw-event-client` is the entry point in the event service. 
It provides APIs to make new 
@scaladoc[EventService](csw.event.api.scaladsl.EventService) for scala 
and @scaladoc[IEventService](csw.event.api.javadsl.IEventService) for java.
It takes an @scaladoc[EventStore](csw.event.client.models.EventStore) which could be either
@scaladoc[RedisStore](csw.event.client.models.EventStores.RedisStore) or
@scaladoc[KafkaStore](csw.event.client.models.EventStores.KafkaStore).


Depending on which store is provided to the `EventServiceFactory`, an implementation of `EventService` is returned
 which could be either `KafkaEventService` or `RedisEventService`. The default store is set to `RedisStore`. Hence the service returned by default 
 is `RedisEventService`.
 
Below is the sequence diagram of the event service. It captures the flow from creation of `EventService` via 
`EventServiceFactory` till the publishing/subscription of events to Redis.

![Sequence Diagram](sequence-diagram.png) 

`EventServiceFactory` provides overloads of `make` method which allow creation of `EventService` using host-port 
as well as using `LocationService` to resolve the EventStore. `EventService` provides APIs to create 
@scaladoc[EventPublisher](csw.event.api.scaladsl.EventPublisher) and
@scaladoc[EventSubscriber](csw.event.api.scaladsl.EventSubscriber) which allow users to publish/subscribe to events.
It provides both APIs 

* to make new instances of publisher/subscriber
 
* to use a default instance of publisher/subscriber
 
When to use which API is documented in 
@ref[this section](../../services/event.md#accessing-event-service) of the event service doc.

 
Event Service uses [Redis' PubSub](https://redis.io/topics/pubsub) for publishing and subscribing to events.
And to cater a specific feature of fetching the latest event on subscription, [set operation](https://redis.io/commands/set) of Redis DB is used.

We have created a scala library called "Romaine" to communicate to Redis.

![Event Dependencies](event-layers.png)

## Romaine

Romaine is a redis client library writen by us. It's a Scala library built over Java Redis client library called [Lettuce](https://lettuce.io/) which provides rich APIs over existing functionality offered by Lettuce. 

Romaine offers various APIs:


* **Async API:** Provides asynchronous API (`romaine.async.RedisAsyncApi`) for various redis commands like `get`, `set`, `publish` etc.  


* **Reactive API:** Provides API for Subscription and Pattern-Subscription (`romaine.reactive.RedisSubscriptionApi`).
On subscription, it returns an [Akka Stream](https://doc.akka.io/docs/akka/current/stream/index.html) of Events which on execution materializes to `RedisSubscription` instance which gives handle to unsubscribe to events.


* **Keyspace API:** Provides APIs to watch [Keyspace Notifications](https://redis.io/topics/notifications) (`romaine.keyspace.RedisKeySpaceApi`).
This is a rich API built on Akka Streams which provides not just the change events that happen on keys (for eg: Update, Removal etc.) but also the old and new values corresponding to those keys.  

Event Service uses `Async API` for publishing and setting the latest event, and `Reactive API` for subscribing to events and patterns.
`Keyspace API` is used in alarm service.

## Event Publishing

Publishing of events involves two things -

* Publish the event in Redis

* Setting the value of event against the event key in redis. This is to cater to a specific requirement of fetching latest event as soon as a new subscription happens.

In case, when Event Server is not available, the Publish APIs would fail with an exception @scaladoc[EventServerNotAvailable](csw.event.api.exceptions.EventServerNotAvailable).
If due to any other reasons, the publishing of events fail, the publish APIs would throw a @scaladoc[PublishFailure](csw.event.api.exceptions.PublishFailure)

## Event Subscription

Subscribing to event keys returns an [Akka Stream](https://doc.akka.io/docs/akka/current/stream/index.html) of events. Subscription to concrete event keys as well as to glob-style patterns is supported.
With pattern subscription, the subscriber receives all the events with event-keys that match the provided pattern. The subscriber gets a handle 0to instance of @scaladoc[EventSubscription](csw.event.api.scaladsl.EventSubscription) which could be used to unsubscribe.  

The subscription API supports subscribing with different modes to control the rate of events you receive. Two modes are provided - RateAdapterMode and RateLimiter mode. Details of when to use which mode could be found @scaladoc[here](csw/event/api/scaladsl/SubscriptionMode).

Subscriber API also provides a `get` API which could be used to fetch the latest events for the specified event keys.

In case, when Event Server is not available, the Subscribe APIs would fail with an exception @scaladoc[EventServiceNotAvailable](csw.event.api.exceptions.EventServiceNotAvailable)

## Architecture

In order to allow components to discover Event Server/Event Store, it is necessary to register it with the Location Service.
Event Server here is referred to the Redis instance(particularly [Redis Sentinel](https://redis.io/topics/sentinel)).

For high availability of event server, we use the Redis Sentinel along with a master and a slave. Master and slave are configured in "replication" mode.

The Sentinel's responsibility is to promote the slave as master when master goes down. It is important to note that when master
goes down, the "location" of Event Server remains the same because the location of Event Server is the location of Sentinel and not of master or slave.
The master and slave Redis instances are dedicated for event, however Sentinel is shared across CSW Redis-based services. 
As Sentinel caters to more than one master, we need to specify which master to connect to for event server.
That is configured in `reference.conf` in `csw-event-client` project. 

@@snip [reference.conf](../../../../../csw-event/csw-event-client/src/main/resources/reference.conf) { #master-configuration }
 

Once location is registered, components and event CLI can resolve Event server location and start publishing/subscribing. 

![architecture](architecture.png)