# Creating an Assembly or Hcd Component

An Assembly or HCD is implemented by extending the `ComponentHandlers` base class. These handlers are executed by an Akka Actor (Top Level Actor or TLA)
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

**Converting a typed actor system to an untyped actor system** 

The `ctx` available to the component is of type `akka.actor.typed.scaladsl.ActorContext` in Scala or `akka.actor.typed.javadsl.ActorContext` 
in Java. This context can be used to get resources such as actor system which is also typed. In order to get the untyped 
version of an actor system or actor references, Akka has  provided some implicit extension methods in Scala and static
methods in Java which can be used by adding the following import: 

`import akka.actor.typed.scaladsl.adapter._`  for Scala and,
`import akka.actor.typed.javadsl.Adapter.*` for Java

