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

CSW comes with a dedicated `csw-testkit` module for supporting tests. This module includes following multiple individual testkits:
 
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
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/scala/example/teskit/TestKitsExampleTest.scala) { #framework-testkit }

Java
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/java/example/testkit/JTestKitsExampleTest.java) { #framework-testkit }

@@@ note

Similarly you can use other testkits. Please refer API docs for more details.

@@@

### Spawning components

`FrameworkTestKit` provides easy way to spawn components in `Container` or `Standalone` mode.
Use `spawnContainer` method provided by `FrameworkTestKit` to start components in container mode and`spawnStandalone` method to start component in standalone mode.

Below example show how to spawn container or component in standalone mode using framework testkit.

Scala
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/scala/example/teskit/TestKitsExampleTest.scala) { #spawn-using-testkit }

Java
:   @@snip [TestKitsExampleTest.scala](../../../../examples/src/test/java/example/testkit/JTestKitsExampleTest.java) { #spawn-using-testkit }

Full source at GitHub

* @github[Scala](/examples/src/test/scala/example/teskit/TestKitsExampleTest.scala)
* @github[Java](/examples/src/test/java/example/testkit/JTestKitsExampleTest.java)

## Test framework integration

### ScalaTest
If you are using ScalaTest then you can extend `csw.testkit.scaladsl.ScalaTestFrameworkTestKit` to have framework test kit automatically start provided services before running tests and shutdown it when the test is complete. 
This is done in `beforeAll` and `afterAll` from the `BeforeAndAfterAll` trait. If you override that method you should call `super.beforeAll` to start services and `super.afterAll` to shutdown the test kit.

### JUnit
If you are using JUnit then you can use `csw.testkit.javadsl.FrameworkTestKitJunitResource` to have the framework test kit automatically start provided services before running tests and shutdown it when the test is complete.

### Supported CSW Services by FrameworkTestKit

`ScalaTestFrameworkTestKit` and `FrameworkTestKitJunitResource` both support starting one or more of the following services.

- `CSWService.LocationServer` | `JCSWService.LocationServer` 
- `CSWService.ConfigServer`   | `JCSWService.ConfigServer`   
- `CSWService.EventServer`    | `JCSWService.EventServer`    
- `CSWService.AlarmServer`    | `JCSWService.AlarmServer`    

Below example show's the usage of `ScalaTestFrameworkTestKit` and `FrameworkTestKitJunitResource` and how you can start above mentioned services as per your need.

Scala
:   @@snip [ScalaTestExampleIntegrationTest.scala](../../../../examples/src/test/scala/example/teskit/ScalaTestIntegrationExampleTest.scala) { #scalatest-testkit }

Java
:   @@snip [JUnitIntegrationExampleTest.scala](../../../../examples/src/test/java/example/testkit/JUnitIntegrationExampleTest.java) { #junit-testkit }

@@@ note

You do not need to externally start any services like event, config, location etc. via `csw-services.sh` script.
Testkits will start required services as a part of initialization. For event and alarm service, it uses `embedded-redis`. 

@@@

## Unit Tests

The goal of unit testing is to break your application into the smallest testable units, and test them individually, isolating 
a specific piece of functionality and ensuring it is working correctly. 
It is always a good idea to write more unit test cases and relatively fewer component and integration tests.
If you want to get an idea of how many tests you should have in different types of testing phases (Unit/Component/Integration), refer this [blog](https://martinfowler.com/articles/practical-test-pyramid.html)

Unit testing simple scala/java classes or objects is straight forward. You can mock external dependencies using Mockito. Refer to the [Mockito](#mockito) section for more details. 

The following links provide guides for testing applications using different modules of Akka:

- [Akka Untyped Actors](https://doc.akka.io/docs/akka/current/testing.html)
- [Akka Typed Actors](https://doc.akka.io/docs/akka/current/typed/testing.html)
- [Akka Streams](https://doc.akka.io/docs/akka/current/scala/stream/stream-testkit.html)

## Multi-JVM Tests

Testing asynchronous distributed systems requires special tooling/framework support. 
Sbt has a plugin called [sbt-multi-jvm](https://github.com/sbt/sbt-multi-jvm) which helps to test systems across multiple JVMs or machines.
This is especially useful for integration testing where multiple systems communicate with each other.

You can find more details on multi-JVM tests [here](https://doc.akka.io/docs/akka/current/multi-jvm-testing.html).

You can also refer [csw](https://github.com/tmtsoftware/csw) for writing your own multi-JVM tests. For example: @github[CommandServiceTest.scala](/csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala)  

In case you want to run your multi-JVM tests across machines, refer this multi-node testing guide [here](https://doc.akka.io/docs/akka/current/multi-node-testing.html). 

## Mockito

Mocks are used so that unit tests can be written independent of dependencies.  
[csw](https://github.com/tmtsoftware/csw) uses [Mockito](http://site.mockito.org/) for writing unit tests.
ScalaTest comes with [MockitoSugar](http://www.scalatest.org/user_guide/testing_with_mock_objects#mockito) trait which provides some basic syntax sugar for Mockito.

For example: @github[ContainerBehaviorTest.scala](/csw-framework/src/test/scala/csw/framework/internal/container/ContainerBehaviorTest.scala)

## Acceptance Tests

This section explains how and where csw maintains and executes acceptance tests. 
If you are a component writer and want to maintain acceptance tests, you can create a repo similar to [csw-acceptance](https://github.com/tmtsoftware/csw-acceptance) and update dependencies, projects as per your need. 

As required by TMT Systems Engineering, the acceptance pipeline runs all the existing Java and Scala tests from [csw](https://github.com/tmtsoftware/csw) repo on published bintray binaries rather than directly on source code.

More information can be found [here](https://github.com/tmtsoftware/csw-acceptance/blob/master/README.md).

Below are the two separate Jenkins pipelines to run `csw` acceptance tests:

1. [Acceptance Dev Pipeline](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev-nightly-build/)
    - Automatically triggered every night to get fast feedback and intended for developer's visibility.
    
2. [Acceptance Release Pipeline](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/)
    - Automatically triggered on completion of [csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline.
    - `csw-prod-release` pipeline published CSW artifacts to bintray, and must be manually triggered by an administrator.

@@@ note { title=Note }

[csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline is responsible for following tasks:

- build and run `csw` tests
- publish binaries to bintray
- publish paradox documentation
- publish apps and release notes to github releases
- trigger [acceptance-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/) pipeline 

@@@

Acceptance pipelines can also be triggered manually via an HTTP end point, for STIL acceptance tesing, for example. 
Using the security token obtained from the Jenkins pipeline settings (available upon request), run the `curl` cmd as shown below:

- For triggering [acceptance-dev](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev-nightly-build/) pipeline, run below

```
curl -G 'http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev/buildWithParameters'  \
    --data-urlencode token=$DEV_TOKEN \
    --data-urlencode DEV_VERSION=0.1-SNAPSHOT \
    --data-urlencode BUILD_ENV=DEV
```

- For triggering `acceptance-release` pipeline, run below: (Modify parameters as applicable)

```
curl -G '$REMOTE_JENKINS_URL/job/$JOB_NAME/buildWithParameters' \
    --data-urlencode token=$RELEASE_TOKEN \
    --data-urlencode RELEASE_VERSION=$RELEASE_VERSION \
    --data-urlencode BUILD_ENV=PROD
```
