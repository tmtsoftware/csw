package csw.command.client.internal

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.javadsl.Behaviors
import csw.command.client.SequencerCommandServiceFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{QueryFinal, SubmitSequenceAndWait}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class SequencerCommandServiceImplTest
    extends ScalaTestWithActorTestKit
    with FunSuiteLike
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  test("should submit sequence to the sequencer") {
    val sequence   = Sequence(Setup(Prefix("csw.move"), CommandName("command-1"), None))
    val sequenceId = sequence.runId

    val submitResponse: SubmitResponse     = Completed(Id())
    val queryFinalResponse: SubmitResponse = Completed(sequenceId)

    val sequencer = spawn(Behaviors.receiveMessage[SequencerMsg] {
      case SubmitSequenceAndWait(`sequence`, replyTo) =>
        replyTo ! submitResponse
        Behaviors.same
      case QueryFinal(`sequenceId`, replyTo) =>
        replyTo ! queryFinalResponse
        Behaviors.same
      case _ => Behaviors.same
    })

    val location =
      AkkaLocation(AkkaConnection(ComponentId("sequencer", ComponentType.Sequencer)), Prefix("iris.x.y"), sequencer.toURI)

    val sequencerCommandService = SequencerCommandServiceFactory.make(location)

    sequencerCommandService.submitAndWait(sequence).futureValue should ===(submitResponse)
    sequencerCommandService.queryFinal(sequenceId).futureValue should ===(queryFinalResponse)
  }
}
