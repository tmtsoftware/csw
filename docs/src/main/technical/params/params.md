# Params

The `csw-params` project contains models like Events, Commands, ParameterSet etc which are cross-compiled
to Scala and [Scala.js](https://www.scala-js.org/). 
CSW Framework and CSW Services like Command Service, Event Service, Alarm Service etc
rely on this project for these models.
Since these models are needed across platforms like C/Cpp, Scala/Java, Python etc
this project also contains serialization support for these models. 
[Cbor](https://cbor.io/) is used as default for Serialization and Deserialization of these models. 
Comparisons were done with other popular formats like [Protocol Buffers](https://protobuf.dev),
[Message Pack](https://msgpack.org/index.html) before finalizing on Cbor. 
Cbor was finalized as it offers better performance and has much lesser overhead of maintenance as compared to protocol buffers.

## Cbor

We use scala [Borer](https://github.com/sirthias/borer) library for cbor. 
@scaladoc[CborSupport](csw/params/core/formats/ParamCodecs$) object in `csw-params` contains 
codecs (encoders and decoders) for all the models defined in this project that need to be serialized.

`CborSupport` needs to be imported wherever you need to encode/decode a model. This makes 
all the implicit codecs defined in `CborSupport` available in the current scope.
When any new models are added, their corresponding codecs also need to be added in the `CborSupport`.

Cbor offers Array-Based Encoding and Map-Based Encoding.
Map-based encoding carries keys/labels along with values unlike array-based encoding which encodes only values.
We use the Map-Based encoding since it makes it easy to debug the problems if any in the encoded cbor object.
Though the size of map based encoded cbor objects is slightly more than that of array-based,
it does not affect the performance as much.


@scaladoc[AdtCbor](csw/params/core/formats/AdtCbor) is a helper trait to encode and decode 
ADTs (Abstract Data Types) with Cbor. This is needed due to the way ADTs are encoded in Borer library. 
Having this abstraction ensures all the types in an ADT (Base types plus concrete types) are encoded in a similar manner.
In Borer, encoding of base-types carries the type information which differs than the way concrete-types are 
encoded (without type information).
The problem is described in detail in the issue [here](https://github.com/sirthias/borer/issues/17).
Going via `AdtCbor` ensures the Adt types(base as well concrete) are all encoded as **Base-type** containing
type information.
So if you are writing new types which are ADTs, make sure you add your own helper next to 
@scaladoc[EventCbor](csw/params/core/formats/EventCbor$) or 
@scaladoc[CommandCbor](csw/params/core/formats/CommandCbor$) to ensure proper encoding and 
decoding of ADTs.

 
Usage of Cbor for Event encoding and decoding is shown below:

Scala
:   @@snip [EventsExample.scala](../../../../../examples/src/test/scala/example/params/EventsExample.scala) { #cbor }

Java
:   @@snip [JEventsExample.java](../../../../../examples/src/test/java/example/params/JEventsExample.java) { #cbor }
  

## Tooling

A number of tools are available around Cbor for diagnostic purposes.
This [link](https://cbor.io/tools.html) contains list of all the tools available.
A couple of useful utilities are available [here](https://github.com/cabo/cbor-diag).
Follow the readme for details regarding installation and usage.
 
Utilities like `cbor2json` or `cbor2diag` could be very helpful while debugging. 
Often in development/debugging we wish to look at the actual encoded object,
but since cbor is a binary format, looking at it does not help in debugging.
That's when these tools come in handy.

For example:

This will return the json representation of the given Cbor object.
 
```
cbor2json.rb csw-params/shared/src/test/cbor/data/event.cbor
```

A lot of other similar utilities are present in the same package which you can play with.


Cbor is schema-less. But you could define a schema (.cddl) and validate it against your cbor object.
This is very similar to the json format. Json does not have a predefined schema, but you can define your schema
and validate an object against it.

[CDDL](https://rubygems.org/gems/cddl/versions/0.8.8) tool allows you to give a schema and a cbor/json object,
and it will validate and tell you if the object adheres to the given schema.

`cddl csw-params/shared/src/test/cbor/schema/event_command.cddl validate csw-params/shared/src/test/cbor/data/event.cbor`

Cddl schema for events and commands has been defined [here]($github.base_url$/csw-params/shared/src/test/cbor/schema/event_command.cddl.txt).