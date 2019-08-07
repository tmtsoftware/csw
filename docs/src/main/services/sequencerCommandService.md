# Sequencer Command Service

Sequencer command service provides ability to send `Sequence` of commands to a running `Sequencer`. It sends the provided sequence
  to sequencer and returns `Future[Submit Response]`.

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
To create Sequencer Command Service, you need to resolve `AkkaLocation` of the `Sequencer` using `LocationService`. Pass the resolved
location to `SequencerCommandServiceFactory` which will return handle of `SequencerCommandService`. 

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #create-sequence-command-service }

## Submitting sequence to a sequencer

To submit sequence to Sequencer, `SequencerCommandService` provides `submit` api. Submit method takes a sequence and returns
 `Future[SubmitResponse]`. It starts execution of the sent sequence and returns `Future[Submit Response]` which will complete
  when sequence is finished or Sequencer is not available.

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #submit-sequence }
