## Events

Events are data and each type of event has a unique purpose and unique information, but they all share same structural features. All events have **EventInfo** and **ParameterSet**.

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

StatusEvent is used to describe the status or state of a component for system monitoring.

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #statusevent }

### Observe Event

ObserveEvent is used to describe an event within a standardized data acquisition process. Published only by Science Detector Assemblies.

Scala
:   @@snip [EventsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/EventsTest.scala) { #observeevent }

### System Event

SystemEvent is used to describe a demand or algorithm input for a component.

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