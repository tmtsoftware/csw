## Result

Components use **Results** to return results in the form of a **ParameterSet**. 

### RunId

Represents a unique id for each running command. To create new RunId, use parameter-less `apply` method, which will do required initialization create and store a new **UUID** automatically.

Scala
:   @@snip [ResultTest.scala](../../../../../examples/src/test/scala/csw/services/messages/ResultTest.scala) { #runid }

Creating a Result requires:

 * **[RunId](result.html)**
 * **[ObsId](commands.html#ObsId)**
 * **[Prefix](commands.html#Prefix)**
 * **[Set[Parameter]](keys-parameters.html)**

Scala
:   @@snip [ResultTest.scala](../../../../../examples/src/test/scala/csw/services/messages/ResultTest.scala) { #result }

### JSON serialization
State variables can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize DemandState and CurrentState.

Scala
:   @@snip [ResultTest.scala](../../../../../examples/src/test/scala/csw/services/messages/ResultTest.scala) { #json-serialization }

### Unique Key constraint

By choice, a ParameterSet in **Result** will be optimized to store only unique keys. In other words, trying to store multiple keys with same name, will be automatically optimized by removing duplicates.

@@@ note

Parameters are stored in a Set, which is an unordered collection of items. Hence, it's not predictable whether first or last duplicate copy will be retained. Hence, cautiously avoid adding duplicate keys.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [ResultTest.scala](../../../../../examples/src/test/scala/csw/services/messages/ResultTest.scala) { #unique-key }
