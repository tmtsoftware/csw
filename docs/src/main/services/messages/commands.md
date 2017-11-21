## Commands

### ObsId

It is an observation Id and can be constructed by creating instance of `ObsId`. 

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #obsid }

### Prefix

It is a combination of [Subsystem](subsystem.html) and Subsystem's prefix. Component developer should supply a valid prefix string and the subsystem will be automatically parsed from it. 

See below examples:

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #prefix }

### Setup Command

This command is used to describe a goal that a system should match. Component developer will require to supply following arguments to create `Setup` command.

 * **[ObsId:](commands.html#ObsId)**  An observation Id.
 * **Prefix:**
 * **paramSet:** Optional Set of Parameters. Default is empty.
 
Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #setup }
 
 
### Observe Command
This command describes a science observation. Sent only to Science Detector Assemblies and Sequencers.

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #observe }

### Wait Command
This command causes a Sequencer to wait until notified.

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #wait }

### JSON serialization
Commands can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Setup, Observe and Wait commands.

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #json-serialization }

### Unique Key constraint

By choice, a ParameterSet in either **Setup, Observe,** or **Wait** command will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #unique-key }