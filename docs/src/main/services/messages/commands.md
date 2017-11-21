## Commands

### ObsId

It is an observation Id and can be constructed by creating instance of `ObsId`. 

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #obsid }

### Setup

This command is used to describe a goal that a system should match. Component developer will require to supply following arguments to create `Setup` command.

 * **[ObsId:](commands.html#ObsId)**  An observation Id.
 * **Prefix:** It is a combination of [Subsystem](subsystem.html) and Subsystem's prefix.
 * **paramSet:** Set of Parameters.
 
Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #setup }
 
 
### Observe
This command describes a science observation. Sent only to Science Detector Assemblies and Sequencers.

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #observe }

### Wait
This command causes a Sequencer to wait until notified.

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/CommandsTest.scala) { #wait }
