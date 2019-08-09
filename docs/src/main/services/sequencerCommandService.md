# Sequencer Command Service

The Sequencer Command Service provides the ability to send a `Sequence` of commands to a running `Sequencer`. It sends the provided Sequence
  to a Sequencer and returns `Future[Submit Response]`.

## Dependencies

The Sequencer Command Service comes bundled with the Framework, no additional dependency is needs to be added to your `build.sbt`
file if you are using it. To use the Sequencer Command Service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-command" % "$version$"
    ```
    @@@
    
## Creating Sequencer Command Service
To create a Sequencer Command Service, you need to resolve the `AkkaLocation` of the `Sequencer` using the Location Service. Pass the resolved
location to a `SequencerCommandServiceFactory`, which will return the handle of a `SequencerCommandService`. 

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #create-sequence-command-service }

## Submitting Sequence to a Sequencer

To submit a Sequence to Sequencer, `SequencerCommandService` provides a `submit` API. The submit method takes a Sequence and returns a
 `Future[SubmitResponse]`. It starts execution of the sent Sequence and returns `Future[Submit Response]`, which will complete
  when Sequence is finished or an error if the Sequencer is not available.

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #submit-sequence }
