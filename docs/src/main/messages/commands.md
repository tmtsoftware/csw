## Commands

### ObsId

It is an observation Id and can be constructed by creating instance of `ObsId`. 

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #obsid }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #obsid }

### Prefix

Identifies a [Subsystem](subsystem.html) in TMT observatory. Component developer should supply a valid prefix string and the subsystem will be automatically parsed from it. 

See below examples:

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #prefix }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #prefix }

### Setup Command

This command is used to describe a goal that a system should match. Component developer will require to supply following arguments to create `Setup` command.

 * **[ObsId:](commands.html#ObsId)**  An observation Id.
 * **Prefix:**
 * **paramSet:** Optional Set of Parameters. Default is empty.
 
Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #setup }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #setup }
 
 
### Observe Command

This command describes a science observation. Sent only to Science Detector Assemblies and Sequencers.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #observe }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #observe }

### Wait Command

This command causes a Sequencer to wait until notified.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #wait }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #wait }

### JSON serialization
Commands can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Setup, Observe and Wait commands.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #json-serialization }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #json-serialization }

### Unique Key constraint

By choice, a ParameterSet in either **Setup, Observe,** or **Wait** command will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #unique-key }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #unique-key }

### Cloning a command

A `cloneCommand` method is available for all commands which can be used to create a new command from existing parameters,
but with a new RunId. 

@@@ note

Any command that is sent needs to have a unique Id as this Id is the one against which the status of the command is 
maintained at the recipient end. This Id can thus be used to query and subscribe the status of the respective command.

@@@  

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #clone-command }

Java
:   @@snip [JCommandsTest.java](../../../../examples/src/test/java/csw/services/messages/JCommandsTest.java) { #clone-command }


## Source code for examples

* @github[Scala Example](/examples/src/test/scala/csw/services/messages/CommandsTest.scala)
* @github[Java Example](/examples/src/test/java/csw/services/messages/JCommandsTest.java)