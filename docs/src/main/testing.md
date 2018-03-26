# Testing

This page will help you getting started with testing your applications. 

## Unit Tests

The goal of unit testing is to segregate smallest testable units of your application and test that it is working correctly. 
It is always good idea to write more number of unit test cases and relatively less component and integration tests.
If you want to get an idea of how many tests you should have in different types of testing phases (Unit/Component/Integration), refer this [blog](https://martinfowler.com/articles/practical-test-pyramid.html)

Unit testing simple scala/java classes or objects is straight forward. You can mock external dependencies using Mockito. Refer @ref:[Mockito](./testing.md#mockito) section for more details.

Follow below guides for testing your application which uses different modules of akka:

- [Akka Untyped Actors](https://doc.akka.io/docs/akka/2.5/testing.html)
- [Akka Typed Actors](https://doc.akka.io/docs/akka/2.5/typed/testing.html)
- [Akka Streams](https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-testkit.html)


## Acceptance Tests

This section explains how and where csw-prod maintains and execute acceptance tests. 
If you are a component writer and want to maintain acceptance tests, you can create a repo similar to [csw-acceptance](https://github.com/tmtsoftware/csw-acceptance).  
and update dependencies, projects as per your need. 

As part of acceptance tests, we run all the existing java and scala tests from [csw-prod](https://github.com/tmtsoftware/csw-prod) repo on published bintray binaries rather than directly on source code.

More information can be found [here](https://github.com/tmtsoftware/csw-acceptance/blob/master/README.md).

Below are the two separate jenkins pipelines to run `csw-prod` acceptance tests:

- [acceptance-dev-nightly-build](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev-nightly-build/)
    - Auto triggered every night to get fast feedback and intended for developer's visibility.
    
- [acceptance-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/)
    - Auto triggered on completion of [csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline.
    - Admin needs to manually trigger `csw-prod-release` pipeline.
    
@@@ note { title=Note }

[csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline is responsible for following tasks:

- build `csw-prod`
- run tests
- publish binaries to bintray
- publish paradox documentation
- publish apps and release notes to github releases
- trigger [acceptance-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/) pipeline 

@@@
    
## Multi-JVM Tests

Testing asynchronous distributed systems requires special tooling/framework support. 
Sbt has a plugin called [sbt-multi-jvm](https://github.com/sbt/sbt-multi-jvm) which helps to test systems across multiple JVMs or machines.
This is especially useful for integration testing where multiple systems communicate with each other.

You can find more details on multi jvm tests [here](https://doc.akka.io/docs/akka/2.5/multi-jvm-testing.html).

You can also refer [csw-prod](https://github.com/tmtsoftware/csw-prod) for writing your own multi JVM tests.

For example: @github[CommandServiceTest.scala](/csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala)  

In case you want to run your Multi-JVM tests across machines, you need to add below dependency in `build.sbt`:

`libraryDependencies    += "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.11" % Test`

Then you can simply run following command to execute tests across machines:

`sbt <project>/multiNodeTest`

One of the pre-requisite before running above command is that, you should have a file named `multi-node-test.hosts` at root level 
which contains a sequence of hosts to use for running the test, on the form `user@host:java` where host is the only required part.
Also make sure, you are able to `ssh` to these machines without password (password less ssh should be enabled).

Refer multi node testing guide for more details [here](https://doc.akka.io/docs/akka/2.5/multi-node-testing.html). 

## Mockito

[csw-prod](https://github.com/tmtsoftware/csw-prod) uses [Mockito](http://site.mockito.org/) for writing unit tests.
ScalaTest comes with [MockitoSugar](http://www.scalatest.org/user_guide/testing_with_mock_objects#mockito) trait which provides some basic syntax sugar for Mockito.

For example: @github[ContainerBehaviorTest.scala](/csw-framework/src/test/scala/csw/framework/internal/container/ContainerBehaviorTest.scala)
