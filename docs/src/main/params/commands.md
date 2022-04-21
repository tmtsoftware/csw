# Commands

Commands are parameter sets called Setup, Observe, and Wait. A command is created with the source of the command, 
given by a prefix, the name of the command, and an optional ObsId. Parameters are added to the command as needed.
As the ESW design is developed, these command structures may evolve.

## ObsId

An `ObsID`, or observation ID, indicates the observation the command is associated with. 
It can be constructed by creating an instance of `ObsId`. 

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #obsid }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #obsid }

## Source

The source of the command is given by the prefix, which should be the full name of the component sending the command.
A prefix can be constructed with a string, but must start with a valid subsystem as in [Subsystem](subsystem.html).
A component developer should supply a valid prefix string and the subsystem will be automatically parsed from it.
An example of a valid string prefix is "nfiraos.ncc.trombone".

See below examples:

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #prefix }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #prefix }

## CommandName

Each command has a name given as a string. The `CommandName` object wraps the string name. The string should be
continuous with no spaces.

## Setup Command

This command is used to describe a goal that a system should match. The component developer is required to supply 
following arguments to create a `Setup` command.

 
 * **[Source:](commands.html#Source)** The source of the command is given by the prefix, which should be the full name of the component sending the command.
 * **[CommandName:](commands.html#CommandName)** a simple string name for the command (no spaces)
 * **[ObsId:](commands.html#ObsId)**  an optional observation Id.
 * **paramSet:** Optional Set of Parameters. Default is empty.
 
Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #setup }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #setup }
 
 
## Observe Command

This command describes a science observation. It is intended to only be sent to Science Detector Assemblies and Sequencers.

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #observe }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #observe }

## Wait Command

This command causes a Sequencer to wait until notified.  It can only be sent to Sequencers.

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #wait }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #wait }

## JSON serialization
Commands can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Setup, Observe and Wait commands.

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #json-serialization }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #json-serialization }

## Unique Key constraint

By design, a ParameterSet in a **Setup, Observe,** or **Wait** command is optimized to store only unique keys. 
When using `add` or `madd` methods on commands to add new parameters, if the parameter being added has a key which is already present in the `paramSet`,
the already stored parameter will be replaced by the given parameter. 
 
@@@ note

If the `Set` is created by component developers and given directly while creating a command, then it will be the responsibility of component developers to maintain uniqueness with
parameters based on key.

@@@ 

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #unique-key }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #unique-key }

## Cloning a Command

In order to track the completion of a command, every command that is sent must have a unique `RunId`.
If you wish to resubmit a previously sent Setup, the `cloneCommand` method must be used prior to submission
to create a new command from existing parameters, but with a new `RunId`.

Scala
:   @@snip [CommandsExample.scala](../../../../examples/src/test/scala/example/params/CommandsExample.scala) { #clone-command }

Java
:   @@snip [JCommandsExample.java](../../../../examples/src/test/java/example/params/JCommandsExample.java) { #clone-command }


# Source Code for Examples

* [Scala Example]($github.base_url$/examples/src/test/scala/example/params/CommandsExample.scala)
* [Java Example]($github.base_url$/examples/src/test/java/example/params/JCommandsExample.java)