# Events

Events are the most basic type of asynchronous notification in TMT when an activity occurs
somewhere in the TMT system and other components need to be notified. Each type of event has a unique
purpose and unique information, but they all share same structural features. 
All events have **EventInfo** and a **ParameterSet**.

@@@ note

The `csw-params` library offers out of the box support to serialize Events using **Cbor**, so that events can be produced and
consumed by JVM (Java virtual machine) as well as Non-JVM applications.

For more on Cbor, refer to the @ref:[technical doc](../technical/params/params.md).

@@@

## EventTime
Each event includes its time of creation in UTC format. You can access that eventTime as follows:

Scala
: @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #eventtime }

Java
: @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #eventtime }
   
## System Event

`SystemEvent` is the type used to describe the majority of events in the system. An example is a demand that is
the output of an algorithm in one component that is used as an input to another. `SystemEvent` is also used
to publish internal state or status values of a component that may be of interest to other components in the system.

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #systemevent }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #systemevent }

## Observe Event

ObserveEvent are standardized events used to describe an activities within the data acquisition process. 
These events are typically published by Science Detector Assemblies, which emit ObserveEvents during their exposures 
to signal the occurrence of specific activities/actions during the acquisition of data. 

@@@ note

The example ObserveEvents do not match the descriptions of the ESW Phase 1 review. The model files and documents
can be used to create standard ObserveEvents.

@@@

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #observeevent }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #observeevent }



## JSON Serialization
Events can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Status, Observe and System events.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #json-serialization }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #json-serialization }

## Unique Key Constraint

By choice, a ParameterSet in either **ObserveEvent** or **SystemEvent** event will be optimized to store only unique keys. 
When using `add` or `madd` methods on events to add new parameters, if the parameter being added has a key which is already present in the `paramSet`,
the already stored parameter will be replaced by the given parameter. 
 
@@@ note

If the `Set` is created by component developers and given directly while creating an event, then it will be the responsibility of component developers to maintain uniqueness with
parameters based on key.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #unique-key }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #unique-key }
