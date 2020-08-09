package csw.command.client.internal

import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.SequencerCommandServiceImpl
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.{Prefix, Subsystem}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationLong

class SequencerCommandServiceImplTest extends AnyFunSuiteLike with Matchers with MockitoSugar with ScalaFutures {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-command-system")
  private implicit val timeout: Timeout                           = Timeout(10.seconds)

  test("should submit sequence to the sequencer") {
    val sequence                           = Sequence(Setup(Prefix("csw.move"), CommandName("command-1"), None))
    val queryFinalId                       = Id("queryFinalId")
    val queryId                            = Id("queryId")
    val submitResponse: SubmitResponse     = Started(Id())
    val queryFinalResponse: SubmitResponse = Error(queryFinalId, "Failed")
    val queryResponse: SubmitResponse      = Invalid(queryId, IdNotAvailableIssue(queryId.id))

    val sequencer = system.systemActorOf(
      Behaviors.receiveMessage[SequencerMsg] {
        case SubmitSequence(`sequence`, replyTo) =>
          replyTo ! submitResponse
          Behaviors.same
        case QueryFinal(_, replyTo) =>
          replyTo ! queryFinalResponse
          Behaviors.same
        case Query(_, replyTo) =>
          replyTo ! queryResponse
          Behaviors.same

        case _ => Behaviors.same
      },
      "sequencer-actor"
    )

    val location = AkkaLocation(
      AkkaConnection(ComponentId(Prefix(Subsystem.IRIS, "sequencer"), ComponentType.Sequencer)),
      sequencer.toURI,
      Metadata.empty
    )

    val sequencerCommandService = new SequencerCommandServiceImpl(location)

    sequencerCommandService.submit(sequence).futureValue should ===(submitResponse)
    sequencerCommandService.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
    sequencerCommandService.query(queryId).futureValue should ===(queryResponse)
    sequencerCommandService.queryFinal(queryFinalId).futureValue should ===(queryFinalResponse)
  }
}
