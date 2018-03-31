## State Variables

A states represent a component's internal state. There are two types called `CurrentState` and `DemandState`. 
They both share the same structural features. All state variables have **[Prefix](commands.html#Prefix)** and **ParameterSet**.

The PubSub feature of the HCD provides `CurrentState` values to the PubSub subscriber.

### DemandState

A state variable that indicates the demand or requested state.

Scala
:   @@snip [StateVariablesTest.scala](../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #demandstate }

Java
:   @@snip [JStateVariablesTest.java](../../../../examples/src/test/java/csw/services/messages/JStateVariablesTest.java) { #demandstate }


### CurrentState

A state variable that is published by a component that describes its internal state. Used by Assemblies to determine command completion in Command Service.

Scala
:   @@snip [StateVariablesTest.scala](../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #currentstate }

Java
:   @@snip [JStateVariablesTest.java](../../../../examples/src/test/java/csw/services/messages/JStateVariablesTest.java) { #currentstate }


### JSON Serialization
State variables can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize DemandState and CurrentState.

Scala
:   @@snip [StateVariablesTest.scala](../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #json-serialization }

Java
:   @@snip [JStateVariablesTest.java](../../../../examples/src/test/java/csw/services/messages/JStateVariablesTest.java) { #json-serialization }

### Unique Key Constraint

By choice, a ParameterSet in either **DemandState** or **CurrentState** will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [StateVariablesTest.scala](../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #unique-key }

Java
:   @@snip [JStateVariablesTest.java](../../../../examples/src/test/java/csw/services/messages/JStateVariablesTest.java) { #unique-key }

## Source Code for Examples

* @github[Scala Example](/examples/src/test/scala/csw/services/messages/StateVariablesTest.scala)
* @github[Java Example](/examples/src/test/java/csw/services/messages/JStateVariablesTest.java)