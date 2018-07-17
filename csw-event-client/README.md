Event Service
=========================

The Event Service implement the [publish/subscribe messaging paradigm](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern) where one component publishes an event and all components that have subscribed receive the event.
The advantage of this type of message system is that publishers and subscribers are decoupled. This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.
Publishers can publish regardless of whether there are subscribers, and subscribers can subscribe even if there are no publishers. 
The relationship between publishers and subscribers can be one-to-one, one-to-many, many to one, or even many-to-many. 

Event Service client provides the implementation for publishing and subscribing to an event.

If you want to get started with Event Service, refer the [examples](https://tmtsoftware.github.io/csw-prod/services/event.html).

You can refer to Scala documentation [here](https://tmtsoftware.github.io/csw-prod/api/scala/csw/services/event/api/index.html).

You can refer to Java documentation [here](https://tmtsoftware.github.io/csw-prod/api/java/?/index.html).