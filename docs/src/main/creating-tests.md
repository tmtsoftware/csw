# Creating Tests

## Unit Tests
    
* Unit testing simple scala/java classes or objects is straight forward. You can mock external dependencies using Mockito. Refer @ref:[Mockito](./creating-tests.md#mockito) section for more details.
* Testing akka actors, please refer akka actor testing guide [here](https://doc.akka.io/docs/akka/2.5/testing.html).
* Testing akka typed actors, please refer akka typed actor testing guide [here](https://doc.akka.io/docs/akka/2.5/typed/testing.html).

These guides explain testing actors synchronously and asynchronously.

Akka provides testkit for every akka module independently.

* Testing akka untyped actors, add below testkit dependency in your `build.sbt`: 

`libraryDependencies    += "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test`

* Testing akka typed actors, add below testkit dependency in your `build.sbt`:  

`libraryDependencies    += "com.typesafe.akka" %% "akka-testkit-typed" % "2.5.11" % Test`

* Testing akka streams, add below testkit dependency in your `build.sbt`:  

`libraryDependencies    += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.11" % Test`

* Testing akka http routes, add below testkit dependency in your `build.sbt`:

`libraryDependencies    += "com.typesafe.akka" %% "akka-http-testkit" % "10.1.0" % Test`

## Acceptance Tests

We maintain a separate repository for running Acceptance Tests [here](https://github.com/tmtsoftware/csw-acceptance). 
As a part of acceptance tests, we run all the existing java and scala tests from [csw-prod](https://github.com/tmtsoftware/csw-prod) repo on published bintray binaries rather than directly on source code.

More information can be found [here](https://github.com/tmtsoftware/csw-acceptance/blob/master/README.md).

We maintain below two separate jenkins pipeline to run these tests:

- [acceptance-dev-nightly-build](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev-nightly-build/)
    - Auto triggered every night to get fast feedback and intended for developer's visibility.
    
- [acceptance-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/)
    - This pipeline automatically gets triggered on completion of [csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline.
    - Admin needs to manually trigger `csw-prod-release` pipeline.
    
## Multi-JVM Tests

Testing asynchronous distributed systems requires special tooling/framework support. 
Sbt has a plugin called [sbt-multi-jvm](https://github.com/sbt/sbt-multi-jvm) which helps to test systems across multiple JVMs or machines.
This is especially useful for integration testing where multiple systems communicate with each other.

You can find more details on multi jvm tests [here](https://doc.akka.io/docs/akka/2.5/multi-jvm-testing.html).

You can also refer [csw-prod](https://github.com/tmtsoftware/csw-prod) for writing your own multi JVM tests.

For example: * @github[CommandServiceTest.scala](/csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala)  

In case you want to run your Multi-JVM tests across machines, you need to add below dependency in `build.sbt`:
`libraryDependencies    += "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.11" % Test`

Then you can simply run following command to execute tests across machines:
`sbt <project>/multiNodeTest`

One of the pre-requisite before running above command is that, you should have a file named `multi-node-test.hosts` at root level which contains list of ip address's of machines
where you want to execute your tests. Also make sure, you are able to `ssh` to those machine without password (password less ssh should be enabled).

Refer multi node testing guide for more details [here](https://doc.akka.io/docs/akka/2.5/multi-node-testing.html). 

## Mockito

[csw-prod](https://github.com/tmtsoftware/csw-prod) uses [Mockito](http://site.mockito.org/) for writing unit tests.
ScalaTest comes with [MockitoSugar](http://www.scalatest.org/user_guide/testing_with_mock_objects#mockito) trait which provides some basic syntax sugar for Mockito.

For example: * @github[ContainerBehaviorTest.scala](/csw-framework/src/main/scala/csw/framework/internal/container/ContainerBehaviorTest.scala)
