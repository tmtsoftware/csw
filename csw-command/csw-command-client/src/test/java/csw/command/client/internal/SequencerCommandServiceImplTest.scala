package csw.command.client.internal

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.SequencerCommandServiceImpl
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.commons.{AskProxyTestKit, RandomUtils}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationLong

class SequencerCommandServiceImplTest
    extends AnyFunSuiteLike
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterAll {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-command-system")
  private implicit val timeout: Timeout                           = Timeout(10.seconds)

  private val askProxyTestKit = new AskProxyTestKit[SequencerMsg, SequencerCommandServiceImpl] {
    override def make(actorRef: ActorRef[SequencerMsg]): SequencerCommandServiceImpl = {
      val location = AkkaLocation(
        AkkaConnection(ComponentId(Prefix(IRIS, "sequencer"), ComponentType.Sequencer)),
        actorRef.toURI,
        Metadata.empty
      )

      new SequencerCommandServiceImpl(location)
    }
  }
  import askProxyTestKit._

  val sequence: Sequence                 = Sequence(Setup(Prefix(s"csw.$randomString5"), CommandName(randomString5), None))
  val id: Id                             = Id()
  val queryFinalResponse: SubmitResponse = mock[SubmitResponse]
  val queryResponse: SubmitResponse      = mock[SubmitResponse]

  private def randomString5 = RandomUtils.randomString5()

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  test("should submit sequence to the sequencer | CSW-110") {
    val submitResponse = mock[SubmitResponse]
    withBehavior { case SubmitSequence(`sequence`, replyTo) =>
      replyTo ! submitResponse
    } check { scs =>
      scs.submit(sequence).futureValue should ===(submitResponse)
    }
  }

  test("should submit sequence to the sequencer and wait for final response | CSW-110") {
    val submitResponse = Started(id)
    withBehavior {
      case SubmitSequence(`sequence`, replyTo) => replyTo ! submitResponse
      case QueryFinal(`id`, replyTo)           => replyTo ! queryFinalResponse
    } check { scs =>
      scs.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
    }
  }

  test("should query current state from the sequencer | CSW-110") {
    withBehavior { case Query(`id`, replyTo) =>
      replyTo ! queryResponse
    } check { scs =>
      scs.query(id).futureValue should ===(queryResponse)
    }
  }

  test("should query final response from the sequencer | CSW-110") {
    withBehavior { case QueryFinal(`id`, replyTo) =>
      replyTo ! queryFinalResponse
    } check { scs =>
      scs.queryFinal(id).futureValue should ===(queryFinalResponse)
    }
  }
}
