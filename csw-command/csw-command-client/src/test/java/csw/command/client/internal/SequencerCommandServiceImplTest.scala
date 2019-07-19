package csw.command.client.internal

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.javadsl.Behaviors
import csw.command.client.messages.{ProcessSequence, SequencerMsg}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
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

  private val sequence         = Sequence(Setup(Prefix("test"), CommandName("command-1"), None))
  private val sequenceResponse = Right(Completed(sequence.runId))

  private val mockedBehavior = Behaviors.receiveMessage[SequencerMsg] {
    case ProcessSequence(`sequence`, replyTo) =>
      replyTo ! sequenceResponse
      Behaviors.same
    case _ => Behaviors.same
  }

  private val sequencer = spawn(mockedBehavior)

  private val componentId = ComponentId("sequencer", ComponentType.Sequencer)
  private val location    = AkkaLocation(AkkaConnection(componentId), Prefix("iris.x.y"), sequencer.toURI)
  private val sequencerCS = new SequencerCommandServiceImpl(location)

  test("should submit sequence to the sequencer") {
    sequencerCS.submit(sequence).futureValue should ===(sequenceResponse.value)
  }

}
