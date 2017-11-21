## State variables

States represent a component's state which can be either a present state or a desired state. They all share same structural features. All events have **[Prefix](commands.html#Prefix)** and **ParameterSet**.

### DemandState

A state variable that indicates the demand or requested state.

Scala
:   @@snip [StateVariablesTest.scala](../../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #demandstate }


### CurrentState

A state variable that is published by a component that describes its internal state. Used by Assemblies to determine command completion in Command Service.

Scala
:   @@snip [StateVariablesTest.scala](../../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #currentstate }

### JSON serialization
State variables can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize DemandState and CurrentState.

Scala
:   @@snip [StateVariablesTest.scala](../../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #json-serialization }

### Unique Key constraint

By choice, a ParameterSet in either **DemandState** or **CurrentState** will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [StateVariablesTest.scala](../../../../../examples/src/test/scala/csw/services/messages/StateVariablesTest.scala) { #unique-key }
