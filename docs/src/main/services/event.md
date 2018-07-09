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
