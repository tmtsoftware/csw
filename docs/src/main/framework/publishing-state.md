## Publishing State

A component has access to `currentStatePublisher` which can be used to publish its @ref:[CurrentState](../messages/states.md). Any subscriber of this component will receive the 
published state. 

@@@ note

This feature is provided for optimized communicaton between an Assembly and an HCD only.

@@@

Scala
:   @@snip [SampleComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/framework/SampleComponentHandlers.scala) { #currentStatePublisher }

Java
:   @@snip [JSampleComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #currentStatePublisher }
