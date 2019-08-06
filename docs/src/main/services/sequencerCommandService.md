# Sequencer Command Service

Sequencer Command Service provides an API to submit a sequence to a sequencer.

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-command" % "$version$"
    ```
    @@@
    
## Creating Sequencer Command Service
For creating a Sequencer Command Service, you need an Akka location of a Sequencer.

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #create-sequence-command-service }

## Submitting sequence to a sequencer
There is an API submit which submits the given Sequence to the Sequencer and returns a submit response.

Scala
:   @@snip [SequencerCommandServiceExample.scala](../../../../examples/src/main/scala/example/sequencerCommandService/SequencerCommandServiceExample.scala) { #submit-sequence }
