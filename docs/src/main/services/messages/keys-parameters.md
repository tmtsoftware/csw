## Keys and Parameters

Library offers a flexible and typesafe means to create Parameters to store values  like **primitive types, collection types or domain specific types**. 

A **Parameter** is a collection of Keys and values or binary data. A key is **unique** in a Parameter. A Parameter is **immutable**, in a sense, a modification to an existing Parameter will return a new instance.  

### How to create a Parameter
 
 * choose appropriate KeyType from below tables for your language(Scala/Java).    
 * calling `make` function on KeyType and supplying a String keyName will return a suitably typed Key instance.    
 * explore the overloaded `set` and `->` methods, which will allow you to store values of the based on chosen KeyType. e.g. `JKeyTypes.BooleanKey` will allow storing only `java.lang.Boolean` values.
 
### Primitive datatypes

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| Boolean         | KeyType.BooleanKey          | JKeyTypes.BooleanKey           |
| Character       | KeyType.CharKey             | JKeyTypes.JCharKey             |
| Byte            | KeyType.ByteKey             | JKeyTypes.ByteKey              |
| Short           | KeyType.ShortKey            | JKeyTypes.ShortKey             |
| Long            | KeyType.LongKey             | JKeyTypes.LongKey              |
| Int             | KeyType.IntKey              | JKeyTypes.IntKey               |
| Float           | KeyType.FloatKey            | JKeyTypes.FloatKey             |
| Double          | KeyType.DoubleKey           | JKeyTypes.DoubleKey            |
| String          | KeyType.StringKey           | JKeyTypes.StringKey            |
| Timestamp       | KeyType.TimestampKey        | JKeyTypes.TimestampKey         |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #primitives }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #primitives }

### Arrays

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| ByteArray       | KeyType.ByteArrayKey        | JKeyTypes.ByteArrayKey         |
| ShortArray      | KeyType.ShortArrayKey       | JKeyTypes.ShortArrayKey        |
| LongArray       | KeyType.LongArrayKey        | JKeyTypes.LongArrayKey         |
| IntArray        | KeyType.IntArrayKey         | JKeyTypes.IntArrayKey          |
| FloatArray      | KeyType.FloatArrayKey       | JKeyTypes.FloatArrayKey        |
| DoubleArray     | KeyType.DoubleArrayKey      | JKeyTypes.DoubleArrayKey       |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #arrays }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #arrays }

### Matrices

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| ByteMatrix      | KeyType.ByteMatrixKey       | JKeyTypes.ByteMatrixKey        |
| ShortMatrix     | KeyType.ShortMatrixKey      | JKeyTypes.ShortMatrixKey       |
| LongMatrix      | KeyType.LongMatrixKey       | JKeyTypes.LongMatrixKey        |
| IntMatrix       | KeyType.IntMatrixKey        | JKeyTypes.IntMatrixKey         |
| FloatMatrix     | KeyType.FloatMatrixKey      | JKeyTypes.FloatMatrixKey       |
| DoubleMatrix    | KeyType.DoubleMatrixKey     | JKeyTypes.DoubleMatrixKey      |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #matrices }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #matrices }


### Domain Specific Types

| Primitive       | Scala KeyType               | Java KeyType                   | 
| :-------------: |:--------------------------: | :-----------------------------:|  
| Choice          | KeyType.ChoiceKey           | JKeyTypes.ChoiceKey            |
| RaDec           | KeyType.RaDecKey            | JKeyTypes.RaDecKey             |
| Struct          | KeyType.StructKey           | JKeyTypes.StructKey            |

#### Choice

A key for a choice item similar to an enumeration.

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #choice }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #choice }


#### RaDec

Holds Ra(Right Ascension) and Dec(Declination) values

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #radec }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #radec }


#### Struct

Stores a set of Parameters for telescope and instrument control. Lot of utility functions available for store, add, remove, list Keys and Paramete

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../../examples/src/test/scala/csw/services/messages/KeysAndParametersTest.scala) { #struct }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../../examples/src/test/java/csw/services/messages/JKeysAndParametersTest.java) { #struct }
