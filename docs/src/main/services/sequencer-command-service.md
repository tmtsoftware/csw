# Sequencer Command Service

The @scaladoc[SequencerCommandService](csw/command/api/scaladsl/SequencerCommandService) provides the ability to send a @scaladoc[Sequence](csw/params/commands/Sequence) of commands to a running `Sequencer`. 
A future value of @scaladoc[SubmitResponse](csw/params/commands/CommandResponse/SubmitResponse) is returned on execution of the provided Sequence.

## Dependencies

To use the SequencerCommandService, add this to your `build.sbt` file:

sbt
: @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-command" % "$version$"
    ```
    @@@
    
## Creating SequencerCommandService

To create SequencerCommandService, you need to resolve the @scaladoc[AkkaLocation](csw/location/models/AkkaLocation) of the `Sequencer` using the Location Service. Pass the resolved
location to the @scaladoc[SequencerCommandServiceImpl](csw/command/client/SequencerCommandServiceImpl), which will return the handle of a `SequencerCommandService`. 

Scala
: @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #create-sequencer-command-service }

## Submitting Sequence to a Sequencer

To submit a Sequence to a Sequencer, `SequencerCommandService` provides a `submit` API which takes a Sequence and returns a
`Future[SubmitResponse]`.

If the sequencer is idle, the provided sequence is loaded in the sequencer and
execution of the sequence starts immediately, and a `Started` response is returned.
If the sequencer is already running another sequence, an `Invalid` response is returned.

Scala
: @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #submit-sequence }

`query` or `queryFinal` Apis, as shown above, could be used to query for the sequence result after the sequence is `submit`ted.
`query` returns the current response which could be either final response (eg. `Completed`) or intermediate response (eg. `Started`).
Whereas `queryFinal` will wait for the final response of the sequence for the given `timeout`. This Api will never return an intermediate response.

If you are not interested in initial/intermediate response but only in final response, you can use the `submitAndWait` api which submits
the sequence and waits for the final response if the sequence was successfully `Started`.


Scala
: @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #submitAndWait }