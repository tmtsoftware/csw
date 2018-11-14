# Adding Unit Tests

Unit testing is a fundamental part of programming, and essential component of the TMT quality assurance policy.
TMT CSW extensively uses unit testing for both Scala and Java code, using ScalaTest for the former, and primarily
JUnit for the latter (TestNG is used in one instance for the Event Service).  While this guide will not attempt
to educate the reader on how to use these popular packages, it will serve to show some examples of how tests
can be created for component code and demonstate some tools provided by CSW to simplify and enable integration
of TMT components and other software with CSW and its services.

## CSW Test Kit

CSW provides a set of tools for use by developers called the CSW Test Kit.  This allows the developer to 
start CSW services within the testing environment, so that they can be accessed by the components and/or
applications being tested, as well as the testing fixtures themselves.  It also provides simple methods to start
components or a set of components within a container, as well as an ActorContext to be used if other Actors
are needed to be created in the tests.

More information about testing with CSW and the CSW Test Kit can be found @ref:[here](./testing.md).

#### *Tutorial: Writing unit tests for our components*

In this part of the tutorial, we will write a few unit tests for our components.  These tests are in no way
meant to be comprehensive, but hopefully, they show enough to get you started.

The giter8 template provides the required directory structure, and skeletons for tests of the HCD and Assembly
in both Java and Scala.   It also provides some Component Configuration (ComponentInfo) files for running
each of the HCD and Assembly in standalone mode for both languages.  They are there for convenience, but 
may not be required depending your deployment and testing strategy.  We will be using them in our tutorial.

We will first look at the tests for the Assembly.  As described on the @ref:[Testing Manual page](./testing.md),
the Scala version overrides the CSW-provided superclass `csw.testkit.scaladsl.ScalaTestFrameworkTestKit` to 
get access to the services it needs.  By passing in the needed services in the constructor, those services are
started in the superclass's `beforeAll` method.  In the Java version, we must create an instance of 
`csw.testkit.javadsl.FrameworkTestKitJunitResource` to get access to and start our services, with the
services we want to start passed into the constructor of this object.

Scala
:   @@snip [SampleAssemblyTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/sampleassembly/SampleAssemblyTest.scala) { #intro }

Java
:   @@snip [JSampleAssemblyTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/sampleassembly/JSampleAssemblyTest.java) { #intro }

For our tests, we will want to run the Assembly first.  We will do this in the `beforeAll` method in Scala, and
in a method with a `@BeforeClass` annotation in Java, so that it is run only once, before any of the tests are run.
The Component Configuration files use are the one provided by the giter8 template.
Note that for Scala, we must call the superclass's `beforeAll` method to ensure the services are run.

Scala
:   @@snip [SampleAssemblyTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/sampleassembly/SampleAssemblyTest.scala) { #setup }

Java
:   @@snip [JSampleAssemblyTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/sampleassembly/JSampleAssemblyTest.java) { #setup }

Next, let's add a test.  We will add a simple test that uses the Location Service to make sure the Assembly is
running and resolve the registration information for it.

Scala
:   @@snip [SampleAssemblyTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/sampleassembly/SampleAssemblyTest.scala) { #locate }

Java
:   @@snip [JSampleAssemblyTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/sampleassembly/JSampleAssemblyTest.java) { #locate }

You can try running the test either using sbt (`sbt test` from the project root directory) or directly in the
IDE.  If you are using IntelliJ, you can run the test by right-clicking on the file in the project explorer
and clicking on `Run 'SampleAssemblyTest'` or `Run 'JSampleAssemblyTest'`.  You can also right-click in the class body
or the specific test body, if you want to run a single test.

The Assembly we have written does not have much of a public API, so we'll turn to the HCD now, which has a few
additional things we can test, including the publishing of Events and the handling of commands.

First, we will set up the test fixtures similarly as we did for the Assembly, and add a similar test to show 
the component registers itself with the Location Service on startup.

Scala
:   @@snip [SampleHcdTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/samplehcd/SampleHcdTest.scala) { #setup }

Java
:   @@snip [JSampleHcdTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/samplehcd/JSampleHcdTest.java) { #setup }

Now let's add a test to verify our component is publishing.  We will set up a test subscriber to the
`counterEvent` Events publisihed by the HCD.  Since we cannot guarantee the order in which the
tests are run, we cannot be certain how long the component has been running when this specific test is run.
Therefore, checking the contents of the Events received is tricky.  We will wait a bit at the start of the 
test to ensure we don't get a InvalidEvent, which would be returned if we start our subscription before the
HCD publishes any Events.  Then, after setting up the subscription, we wait 5 seconds to allow the HCD to 
publish two additional Events plus the one we receive when the subscription starts.  We will look at the counter
value of the first `counterEvent` to determine what the set of counter values we expect to get in our subscription.

Scala
:   @@snip [SampleHcdTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/samplehcd/SampleHcdTest.scala) { #subscribe }

Java
:   @@snip [JSampleHcdTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/samplehcd/JSampleHcdTest.java) { #subscribe }

Next, we'll add a test for command handling in the HCD.  The HCD supports a "sleep" command, which sleeps
some amount of seconds as specified in the command payload, and then returns a `CommandResponse.Completed`.
We will specify a sleep of 5 seconds, and then check that we get the expected response.  Note that the 
obtaining a `CommandService` reference requires an Akka Typed Actor System, so our code will create one
using the Actor System provided by the Test Kit.

Scala
:   @@snip [SampleHcdTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/samplehcd/SampleHcdTest.scala) { #submit }

Java
:   @@snip [JSampleHcdTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/samplehcd/JSampleHcdTest.java) { #submit }

Finally, we will show an example of tests that check that exceptions are thrown when expected.  We will do this
by using the "sleep" command, but failing to wait long enough for the sleep to complete.  This causes a 
`TimeoutException` in Scala, and an `ExecutionException` in Java, and our tests check to see that these types
are in fact thrown.

Scala
:   @@snip [SampleHcdTest.scala](../../../../examples/src/test/scala/org/tmt/nfiraos/samplehcd/SampleHcdTest.scala) { #exception }

Java
:   @@snip [JSampleHcdTest.java](../../../../examples/src/test/java/org/tmt/nfiraos/samplehcd/JSampleHcdTest.java) { #exception }



