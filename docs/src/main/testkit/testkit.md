# Testing

## Dependencies

To use Csw Testkit, you must add the following dependency in your project:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-testkit" % "$version$"
    ```
    @@@

## Introduction

CSW comes with a dedicated module `csw-testkit` for supporting tests. This module includes following multiple individual testkits:
 
 - `LocationTestKit` : starts and stops location server
 - `ConfigTestKit` : starts and stops config server
 - `EventTestKit` : starts and stops event service (Note : This uses `embedded-redis` to start redis sentinel and master) 
 - `AlarmTestKit` : starts and stops alarm service (Note : This uses `embedded-redis` to start redis sentinel and master)
 - `FrameworkTestKit` : in most of the cases, you will end up using this testkit. `FrameworkTestKit` is created by composing all the above mentioned testkits.
    Hence it supports starting and stopping all provided csw services. 
    
@@@ note

All the testkits requires location server to be up and running. Hence first thing all testkits does is to start location server.
You do not need to start it explicitly.

@@@

## TestKits

When you really want a granular level access to testkits then only you would want to use `LocationTestKit`|`ConfigTestKit`|`EventTestKit`|`AlarmTestKit`|`FrameworkTestKit` directly.
You can create instance of `FrameworkTestKit` as shown below:

Scala
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/scala/csw/teskit/TestKitsExampleTest.scala) { #framework-testkit }

Java
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/java/csw/testkit/JTestKitsExampleTest.java) { #framework-testkit }

@@@ note

Similarly you can use other testkits. Please refer API docs for more details.

@@@

### Spawning components

`FrameworkTestKit` provides easy way to spawn components in `Container` or `Standalone` mode.
Use `spawnContainer` method provided by `FrameworkTestKit` to start components in container mode and
`spawnStandaloneComponent` method to start component in standalone mode.

Below example show how to spawn container or component in standalone mode using framework testkit.

Scala
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/scala/csw/teskit/TestKitsExampleTest.scala) { #spawn-using-testkit }

Java
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/java/csw/testkit/JTestKitsExampleTest.java) { #spawn-using-testkit }

Full source at GitHub
* @github[Scala](/examples/src/test/scala/csw/teskit/TestKitsExampleTest.scala)
* @github[Java](/examples/src/test/java/csw/testkit/JTestKitsExampleTest.java)