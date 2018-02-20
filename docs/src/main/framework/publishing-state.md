## Publishing State

A component has access to `currentStatePublisher` which can be used to publish its @ref:[CurrentState](../services/messages/states.md). Any subscriber of this component will receive the 
published state. 

Scala
:   @@snip [SampleComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/framework/SampleComponentHandlers.scala) { #currentStatePublisher }

Java
:   @@snip [JSampleComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #currentStatePublisher }
