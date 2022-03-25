# State Variables

State variables are used when an Assembly wants to track the status of a command sent to an HCD using
a matcher.  For more information, see @ref:[Publishing State](../framework/publishing-state.md).

A state represents some aspect of a component's internal state. There are two types called `CurrentState` and `DemandState`. 
They both share the same structural features. All state variables have **[Prefix](commands.html#Prefix)** and **ParameterSet**.

The PubSub feature of the HCD provides `CurrentState` values to the PubSub subscriber.

## DemandState

A state variable that indicates the demand or requested state.

Scala
:   @@snip [StateVariablesExample.scala](../../../../examples/src/test/scala/example/params/StateVariablesExample.scala) { #demandstate }

Java
:   @@snip [JStateVariablesExample.java](../../../../examples/src/test/java/example/params/JStateVariablesExample.java) { #demandstate }


## CurrentState

A state variable that is published by a component that describes its internal state. Used by Assemblies to determine command completion in Command Service.

Scala
:   @@snip [StateVariablesExample.scala](../../../../examples/src/test/scala/example/params/StateVariablesExample.scala) { #currentstate }

Java
:   @@snip [JStateVariablesExample.java](../../../../examples/src/test/java/example/params/JStateVariablesExample.java) { #currentstate }


## JSON Serialization
State variables can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize DemandState and CurrentState.

Scala
:   @@snip [StateVariablesExample.scala](../../../../examples/src/test/scala/example/params/StateVariablesExample.scala) { #json-serialization }

Java
:   @@snip [JStateVariablesExample.java](../../../../examples/src/test/java/example/params/JStateVariablesExample.java) { #json-serialization }

## Unique Key Constraint

By design, a ParameterSet in either **DemandState** or **CurrentState** will be optimized to store only unique keys.
When using `add` or `madd` methods on events to add new parameters, if the parameter being added has a key which is already present in the `paramSet`,
the already stored parameter will be replaced by the given parameter. 
 
@@@ note

If the `Set` is created by component developers and given directly while creating an event, then it will be the responsibility of component developers to maintain uniqueness with
parameters based on key.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [StateVariablesExample.scala](../../../../examples/src/test/scala/example/params/StateVariablesExample.scala) { #unique-key }

Java
:   @@snip [JStateVariablesExample.java](../../../../examples/src/test/java/example/params/JStateVariablesExample.java) { #unique-key }

## Source Code for Examples

* [Scala Example]($github.base_url$/examples/src/test/scala/example/params/StateVariablesExample.scala)
* [Java Example]($github.base_url$/examples/src/test/java/example/params/JStateVariablesExample.java)