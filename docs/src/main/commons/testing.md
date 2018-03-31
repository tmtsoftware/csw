# Testing

This page will help you getting started with testing your applications. 

## Unit Tests

The goal of unit testing is to break you application into the smallest testable units, and test them individually, isolating 
a specific piece of functionality and ensuring it is working correctly. 
It is always good idea to write more unit test cases and relatively fewer component and integration tests.
If you want to get an idea of how many tests you should have in different types of testing phases (Unit/Component/Integration), refer this [blog](https://martinfowler.com/articles/practical-test-pyramid.html)

Unit testing simple scala/java classes or objects is straight forward. You can mock external dependencies using Mockito. Refer to the [Mockito](#mockito) section for more details. 

The folloowing links provide guides for testing applications using different modules of Akka:

- [Akka Untyped Actors](https://doc.akka.io/docs/akka/current/testing.html)
- [Akka Typed Actors](https://doc.akka.io/docs/akka/current/typed/testing.html)
- [Akka Streams](https://doc.akka.io/docs/akka/current/scala/stream/stream-testkit.html)

## Acceptance Tests

This section explains how and where csw-prod maintains and executes acceptance tests. 
If you are a component writer and want to maintain acceptance tests, you can create a repo similar to [csw-acceptance](https://github.com/tmtsoftware/csw-acceptance) and update dependencies, projects as per your need. 

As required by TMT Systems Engineering, the acceptance pipeline runs all the existing Java and Scala tests from [csw-prod](https://github.com/tmtsoftware/csw-prod) repo on published bintray binaries rather than directly on source code.

More information can be found [here](https://github.com/tmtsoftware/csw-acceptance/blob/master/README.md).

Below are the two separate jenkins pipelines to run `csw-prod` acceptance tests:

1. [Acceptance Dev Pipeline](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-dev-nightly-build/)
    - Auto triggered every night to get fast feedback and intended for developer's visibility.
    
2. [Acceptance Release Pipeline](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/)
    - Auto triggered on completion of [csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline.
    - Admin needs to manually trigger `csw-prod-release` pipeline.

Acceptance pipelines are enabled to be triggered remotely. This is required, so that `csw-prod-release` pipeline can trigger `acceptance-release` pipeline which would be created in different jenkins server hosted in STILL environment. 
In case you want to trigger it via http end point, first you need to have security `token` configured in jenkins pipeline to trigger the job remotely and then run `curl` cmd as shown below:

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

@@@ note { title=Note }

[csw-prod-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-release/) pipeline is responsible for following tasks:

- build and run `csw-prod` tests
- publish binaries to bintray
- publish paradox documentation
- publish apps and release notes to github releases
- trigger [acceptance-release](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/acceptance-release/) pipeline 

@@@
    
## Multi-JVM Tests

Testing asynchronous distributed systems requires special tooling/framework support. 
Sbt has a plugin called [sbt-multi-jvm](https://github.com/sbt/sbt-multi-jvm) which helps to test systems across multiple JVMs or machines.
This is especially useful for integration testing where multiple systems communicate with each other.

You can find more details on multi jvm tests [here](https://doc.akka.io/docs/akka/current/multi-jvm-testing.html).

You can also refer [csw-prod](https://github.com/tmtsoftware/csw-prod) for writing your own multi JVM tests. For example: @github[CommandServiceTest.scala](/csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala)  

In case you want to run your multi jvm tests across machines, refer this multi node testing guide [here](https://doc.akka.io/docs/akka/current/multi-node-testing.html). 

## Mockito

[csw-prod](https://github.com/tmtsoftware/csw-prod) uses [Mockito](http://site.mockito.org/) for writing unit tests.
ScalaTest comes with [MockitoSugar](http://www.scalatest.org/user_guide/testing_with_mock_objects#mockito) trait which provides some basic syntax sugar for Mockito.

For example: @github[ContainerBehaviorTest.scala](/csw-framework/src/test/scala/csw/framework/internal/container/ContainerBehaviorTest.scala)
