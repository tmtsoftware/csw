package csw.command.client.internal

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.javadsl.Behaviors
import csw.command.client.messages.ProcessSequenceError.{DuplicateIdsFound, ExistingSequenceIsInProcess}
import csw.command.client.messages.{ProcessSequence, ProcessSequenceResponse, SequencerMsg}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class SequencerCommandServiceImplTest
    extends ScalaTestWithActorTestKit
    with FunSuiteLike
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  private val sequence = Sequence(Setup(Prefix("test"), CommandName("command-1"), None))
  private var sequenceResponse = ProcessSequenceResponse(Right(Completed(sequence.runId)))


  private val mockedBehavior = Behaviors.receiveMessage[SequencerMsg] {
    case ProcessSequence(`sequence`, replyTo) =>
      replyTo ! sequenceResponse
      Behaviors.same
    case _ => Behaviors.same
  }

  private val sequencer = spawn(mockedBehavior)

  private val componentId             = ComponentId("sequencer", ComponentType.Sequencer)
  private val location                = AkkaLocation(AkkaConnection(componentId), Prefix("iris.x.y"), sequencer.toURI)
  private val sequencerCommandService = new SequencerCommandServiceImpl(location)

  test("should submit sequence to the sequencer") {
    sequencerCommandService.submit(sequence).futureValue should ===(sequenceResponse.response.toOption.get)
  }

  test("should get DuplicateIds error for invalid sequence") {
    sequenceResponse = ProcessSequenceResponse(Left(DuplicateIdsFound))
    sequencerCommandService.submit(sequence).futureValue should ===(
      Error(sequence.runId, "Duplicate command Ids found in given sequence")
    )
  }

  test("should get ExistingSequenceIsInProcess error") {
    sequenceResponse = ProcessSequenceResponse(Left(ExistingSequenceIsInProcess))
    sequencerCommandService.submit(sequence).futureValue should ===(
      Error(sequence.runId, "Submit failed, existing sequence is already in progress")
    )
  }

}
