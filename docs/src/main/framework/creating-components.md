## Creating an Assembly or Hcd Component

A component is implemented by extending the `ComponentHandlers` base class. These handlers are executed under an actor (Top Level Actor or TLA)
defined in the framework which handles the lifecycle and supervision of this component.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #component-handlers-class }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #jcomponent-handlers-class }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #component-handlers-class }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #jcomponent-handlers-class }

@@@ note { title=Note }

**converting typed actor system to untyped actor system** 

The `ctx` available to the component is of type `akka.actor.typed.scaladsl.ActorContext` in scala or `akka.actor.typed.javadsl.ActorContext` 
in java. This context can be used to get resources such as actor system which is also typed. In order to get the untyped 
version of actor system or actor references, akka has  provided some implicit extension methods in scala and static
methods in java which can be used by adding the following import 

`import akka.actor.typed.scaladsl.adapter._`  for scala and,
`import akka.actor.typed.javadsl.Adapter.*` for java

@@@

A component can be created by a factory which extends `ComponentBehaviorFactory` base class and provides a definition of `handlers` method to return the appropriate implementation of `ComponentHandlers`.

Assembly/Scala
:   @@snip [AssemblyComponentBehaviorFactory.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentBehaviorFactory.scala) { #component-factory }

Assembly/Java
:   @@snip [JAssemblyComponentBehaviorFactory.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentBehaviorFactory.java) { #jcomponent-factory }

Hcd/Scala
:   @@snip [HcdComponentBehaviorFactory.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentBehaviorFactory.scala) { #component-factory }

Hcd/Java
:   @@snip [JHcdComponentBehaviorFactory.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentBehaviorFactory.java) { #jcomponent-factory }