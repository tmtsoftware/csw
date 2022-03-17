# Creating an Assembly or Hcd Component

To create a component(Assembly or HCD), `ComponentHandlers` are needed. 
These handlers are executed by an Akka Actor (Top Level Actor or TLA)
defined in the framework which handles the lifecycle and supervision of each component.

There are two ways to create `ComponentHandlers`:

1. By extending `ComponentHandlers`(`JComponentHandlers` for Java) abstract class and implement each handler.
2. By extending `DefaultComponentHandlers`(`JDefaultComponentHandlers` for Java) class and only override handlers those handlers which are needed to change.

Examples for case 1:

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #component-handlers-class }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #jcomponent-handlers-class }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #component-handlers-class }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #jcomponent-handlers-class }


Examples for case 2:

Assembly/Scala
:   @@snip [TCSAssemblyCompHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/TCSAssemblyCompHandlers.scala) { #component-handlers-class }

Assembly/Java
:   @@snip [JTCSAssemblyCompHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JTCSAssemblyCompHandlers.java) { #jcomponent-handlers-class }

Hcd/Scala
:   @@snip [TCSHcdCompHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/TCSHcdCompHandlers.scala) { #component-handlers-class }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JTCSHcdCompHandlers.java) { #jcomponent-handlers-class }

@@@ note { title=Note }

**Converting a typed actor system to an untyped actor system** 

The `ctx` available to the component is of type `akka.actor.typed.scaladsl.ActorContext` in Scala or `akka.actor.typed.javadsl.ActorContext` 
in Java. This context can be used to get resources such as an actor system which is also typed. In order to get the untyped 
version of an actor system or actor references, Akka has  provided some implicit extension methods in Scala and static
methods in Java which can be used by adding the following import: 

* Scala: `import akka.actor.typed.scaladsl.adapter._`
* Java: `import akka.actor.typed.javadsl.Adapter.*`

@@@
