# Events

Events are the most basic type of asynchronous notification in TMT when an activity occurs 
somewhere in the TMT system and other components need to be notified. Each type of event has a unique 
purpose and unique information, but they all share same structural features. 
All events have **EventInfo** and **ParameterSet**.

@@@ note

`csw-messages` library offers out of the box support to serialize Events using **Protobuf**, so that events can be produced and 
consumed by JVM(Java virtual machine) as well as Non-JVM applications.

For more on this [Protobuf support](events.html#protobuf) section below.

@@@

## EventTime
Each event includes its time of creation in UTC format. 
To create an event instance at the current time use the default constructor. For other utility functions, see below examples:

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #eventtime }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #eventtime }
   
## System Event

`SystemEvent` is the type used to describe the majority of events in the system. An example is a demand that is 
the output of an algorithm in one component that is used as an input to another. `SystemEvent` is also used 
to publish internal state or status values of a component that may be of interest to other components in the system.

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #systemevent }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #systemevent }

## Observe Event

ObserveEvent are standardized events used to describe an activities within the data acquisition process. 
These events are ublished only by Science Detector Assemblies, which emit ObserveEvents during their exposures 
to signal the occurrence of specific activities/actions during the acquisition of data. 
Observe Events are published by the detector system using the Event Service.

@@@ note

The current ObserveEvents do not match the descriptions of the ESW Phase 1 review. The model files and documents
can be used to create standard ObserveEvents.

@@@

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #observeevent }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #observeevent }



## JSON Serialization
Events can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Status, Observe and System events.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #json-serialization }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #json-serialization }

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
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #unique-key }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #unique-key }

## Protobuf

Protobuf aka Protocol buffers, are a language-neutral, platform-neutral extensible mechanism for serializing structured data. For more, visit [Protobuf home page](https://developers.google.com/protocol-buffers/)

In TMT observatory, subsystems and components could be running on JVM(Java virtual machine) and Non-JVM platform. This leads to solving a non-trivial problem of a **Non-JVM component** wanting to consume an Event produced by a **JVM component**. Amongst the available options for **data over the wire**, Protobuf was chosen for its performance, data compression and official/unofficial support many mainstream languages.      

`csw-messages` library enhances the Protobuf support, by providing out of the box helper methods, to convert events from/to protobuf binary data.

@@@ note { title="Protobuf Code generation for Non-JVM languages" }

The protobuf schema is defined in @github[csw_protobuf](/csw-messages/src/main/protobuf/csw_protobuf/) directory. The contained **.proto** files can be fed to a protoc compiler in the language of your choice and it will do the required code generation. 

@@@

Here are some examples:

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/messages/EventsTest.scala) { #protobuf }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/csw/messages/JEventsTest.java) { #protobuf }

# Source Code for Examples

* @github[Scala Example](/examples/src/test/scala/csw/messages/EventsTest.scala)
* @github[Java Example](/examples/src/test/java/csw/messages/JEventsTest.java)