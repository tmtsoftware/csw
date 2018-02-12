## Creating an Assembly or Hcd Component

A component is implemented by extending the `ComponentHandlers` base class. These handlers are executed under an actor (Top Level Actor or TLA)
defined in the framework which handles the lifecycle and supervision of this component.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../examples/src/main/scala/csw/services/component/assembly/AssemblyComponentHandlers.scala) { #component-handlers-class }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #jcomponent-handlers-class }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../examples/src/main/scala/csw/services/component/hcd/HcdComponentHandlers.scala) { #component-handlers-class }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #jcomponent-handlers-class }

@@@ note { title=Note }

**converting typed actor system to untyped actor system** 

The `ctx` available to the component is of type `akka.typed.scaladsl.ActorContext` in scala or `akka.typed.javadsl.ActorContext` 
in java. This context can be used to get resources such as actor system which is also typed. In order to get the untyped 
version of actor system or actor references, akka has  provided some implicit extension methods in scala and static
methods in java which can be used by adding the following import 

`import akka.typed.scaladsl.adapter._`  for scala and,
`import akka.typed.javadsl.Adapter.*` for java

@@@

A component can be created by a factory which extends `ComponentBehaviorFactory` base class and provides a definition of `handlers` method to return the appropriate implementation of `ComponentHandlers`.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../examples/src/main/scala/csw/services/component/assembly/AssemblyComponentBehaviorFactory.scala) { #component-factory }

Assembly/Java
:   @@snip [JTromboneAssemblyBehaviorFactory.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyBehaviorFactory.java) { #jcomponent-factory }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../examples/src/main/scala/csw/services/component/hcd/HcdComponentBehaviorFactory.scala) { #component-factory }

Hcd/Java
:   @@snip [JTromboneHcdBehaviorFactory.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdBehaviorFactory.java) { #jcomponent-factory }