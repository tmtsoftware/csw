package example.sequencerCommandService

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object SequencerCommandServiceExample extends App {

  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem)

  // #create-sequence-command-service
  private val connection             = AkkaConnection(ComponentId("sequencer", ComponentType.Sequencer))
  private val location: AkkaLocation = Await.result(locationService.resolve(connection, 5.seconds), 5.seconds).get

  val sequencerCommandService: SequencerCommandServiceImpl = new SequencerCommandServiceImpl(location)
  // #create-sequence-command-service

  // #submit-sequence
  val sequence: Sequence        = Sequence(Setup(Prefix("test.move"), CommandName("command-1"), None))
  implicit val timeout: Timeout = Timeout(10.seconds)

  private val submitResponse: SubmitResponse = Await.result(sequencerCommandService.submitAndWait(sequence), 5.seconds)
  // #submit-sequence

}
