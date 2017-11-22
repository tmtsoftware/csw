## Events

Events are the most basic type of asynchronous notification in TMT when an activity occurs somewhere in the TMT system and other components need to be notified. Each type of event has a unique purpose and unique information, but they all share same structural features. All events have **EventInfo** and **ParameterSet**.

@@@ note

`csw-messages` library offers out of the box support to serialize Events using **Protobuf**, so that events can be produced and consumed by JVM(Java virtual machine) as well as Non-JVM applications.

For more on this [Protobuf support](events.html#protobuf) section below.

@@@

### EventTime
It captures the instance of a time in UTC format. To create current instance of time use default constructor. For other utility functions, see below examples:

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #eventtime }
  
### EventInfo

It captures all the relevant information about an event that occurred during observation. An event can be created by supplying following inputs:

 * **source** which is a [Prefix](commands.html#Prefix) 
 * **eventTime** which is an [EventTime](events.html#EventTime) 
 * **obsId** which is an [ObsId](commands.html#ObsId)
 * **eventId** which is a String representing unique identifier(UUID) which is generated automatically if not supplied.
 
Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #eventinfo }
 

### Status Event

StatusEvents are the published internal state or status values of a component that may be of interest to other components in the system.

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #statusevent }

### Observe Event

ObserveEvent is used to describe an event within a standardized data acquisition process. Published only by Science Detector Assemblies, who emit ObserveEvents during their exposures to signal the occurrence of specific activities/actions during the acquisition of data. Observe Events are published by the detector system using the Event Service.

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #observeevent }

### System Event

SystemEvent is used to describe a demand or other algorithm input from one component to the other.

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #systemevent }

### JSON serialization
Events can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Status, Observe and System events.

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #json-serialization }

### Unique Key constraint

By choice, a ParameterSet in either **StatusEvent, ObserveEvent,** or **SystemEvent** command will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #unique-key }

### Protobuf

Protobuf aka Protocol buffers, are a language-neutral, platform-neutral extensible mechanism for serializing structured data. For more, visit [Protobuf home page](https://developers.google.com/protocol-buffers/)

In TMT observatory, subsystems and components could be running on JVM(Java virtual machine) and Non-JVM platform. This leads to solving a non-trivial problem of a **Non-JVM component** wanting to consume an Event produced by a **JVM component**. Amongst the available options for **data over the wire**, Protobuf was chosen for its performance, data compression and official/unofficial support many mainstream languages.      

`csw-messages` library enhances the Protobuf support, by providing out of the box helper methods, to convert events from/to protobuf binary data.

@@@ note { title="Protobuf Code generation for Non-JVM languages" }

The protobuf schema is defined in @github[csw_protobuf](/csw-messages/src/main/protobuf/csw_protobuf/) directory. The contained **.proto** files can be fed to a protoc compiler in the language of your choice and it will do the required code generation. 

@@@

Here are some examples:

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #protobuf }