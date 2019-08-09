# Keys and Parameters

The library offers a flexible and typesafe means to create Parameters to store values like **primitive types, collection types or domain specific types**. 

A **Parameter** is a Key and Value where the Value must be from a set of defined primitive types including binary data.
The Value of a `Parameter` is always considered to be an Array of the type (i.e. if a single value is stored it is at array location 0). 
A `Parameter` is **immutable**; a modification to an existing `Parameter` will return a new instance. 

A Value can also have `Units`, which must be of the defined types. See @ref:[Units](./units.md) for more information. At this time
Units are informational only--no calculation or conversion support is provided. Some systems may provide a key value with different
units, and receiver can inspect the `Units` to make a decision on how to handle the value.

A **ParameterSet** is a Set of `Parameter`. Various other message types include a `ParameterSet` (e.g. Setup, Event). 
A key is **unique** in a `ParameterSet` since it is a Set. 

## How to Create a Parameter
 
 * choose an appropriate KeyType from the tables below for your language(Scala/Java).    
 * calling the `make` method on KeyType and supplying a String keyName will return a suitably typed Key instance.    
 * exploit the overloaded `set` and `->` methods, which will allow you to store values of the based on chosen KeyType. e.g. `JKeyType.BooleanKey` will allow storing only `java.lang.Boolean` values.
 
## Primitive Datatypes

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| Boolean         | KeyType.BooleanKey          | JKeyType.BooleanKey           |
| Character       | KeyType.CharKey             | JKeyType.JCharKey             |
| Byte            | KeyType.ByteKey             | JKeyType.ByteKey              |
| Short           | KeyType.ShortKey            | JKeyType.ShortKey             |
| Long            | KeyType.LongKey             | JKeyType.LongKey              |
| Int             | KeyType.IntKey              | JKeyType.IntKey               |
| Float           | KeyType.FloatKey            | JKeyType.FloatKey             |
| Double          | KeyType.DoubleKey           | JKeyType.DoubleKey            |
| String          | KeyType.StringKey           | JKeyType.StringKey            |
| Timestamp       | KeyType.TimestampKey        | JKeyType.TimestampKey         |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #primitives }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #primitives }

## Arrays

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| ByteArray       | KeyType.ByteArrayKey        | JKeyType.ByteArrayKey         |
| ShortArray      | KeyType.ShortArrayKey       | JKeyType.ShortArrayKey        |
| LongArray       | KeyType.LongArrayKey        | JKeyType.LongArrayKey         |
| IntArray        | KeyType.IntArrayKey         | JKeyType.IntArrayKey          |
| FloatArray      | KeyType.FloatArrayKey       | JKeyType.FloatArrayKey        |
| DoubleArray     | KeyType.DoubleArrayKey      | JKeyType.DoubleArrayKey       |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #arrays }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #arrays }

## Matrices

| Primitive       | Scala KeyType               | Java KeyType                   |
| :-------------: |:--------------------------: | :-----------------------------:| 
| ByteMatrix      | KeyType.ByteMatrixKey       | JKeyType.ByteMatrixKey        |
| ShortMatrix     | KeyType.ShortMatrixKey      | JKeyType.ShortMatrixKey       |
| LongMatrix      | KeyType.LongMatrixKey       | JKeyType.LongMatrixKey        |
| IntMatrix       | KeyType.IntMatrixKey        | JKeyType.IntMatrixKey         |
| FloatMatrix     | KeyType.FloatMatrixKey      | JKeyType.FloatMatrixKey       |
| DoubleMatrix    | KeyType.DoubleMatrixKey     | JKeyType.DoubleMatrixKey      |

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #matrices }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #matrices }


## Domain Specific Types

| Primitive       | Scala KeyType               | Java KeyType                   | 
| :-------------: |:--------------------------: | :-----------------------------:|  
| Choice          | KeyType.ChoiceKey           | JKeyType.ChoiceKey            |
| Struct          | KeyType.StructKey           | JKeyType.StructKey            |

## Coordinate Types

| Primitive       | Scala KeyType               | Java KeyType                   | 
| :-------------: |:--------------------------: | :-----------------------------:|  
| RaDec           | KeyType.RaDecKey            | JKeyType.RaDecKey             |
| EqCoord         | KeyType.EqCoordKey          | JKeyType.EqCoordKey           |
| SolarSystemCoord| KeyType.SolarSystemCoordKey | JKeyType.SolarSystemCoordKey  |
| MinorPlanetCoord| KeyType.MinorPlanetCoordKey | JKeyType.MinorPlanetCoordKey  |
| CometCoord      | KeyType.CometCoordKey       | JKeyType.CometCoordKey        |
| AltAzCoord      | KeyType.AltAzCoordKey       | JKeyType.AltAzCoordKey        |
| Coord  (*)      | KeyType.CoordKey            | JKeyType.CoordKey             |

@@@ note

Note that since `Coord` is the base trait of the other coordinate types, you can use it as
the key for any of the coordinate types.

@@@

#### Coordinate Types Example

The following example demonstrates the basic usage of the coordinate parameter types:

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #coords }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #coords }


### Choice

A key for a choice item similar to an enumeration.

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #choice }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #choice }


### Struct

Structs can be used to create a hierarchy of parameters. The following snippet also demonstrates some of the utility functions
available for manipulating parameter sets:

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #struct }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #struct }

## Source Code for Examples

* @github[Scala Example](/examples/src/test/scala/example/params/KeysAndParametersTest.scala)
* @github[Java Example](/examples/src/test/java/example/params/JKeysAndParametersTest.java)