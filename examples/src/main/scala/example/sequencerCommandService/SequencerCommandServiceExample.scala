package example.sequencerCommandService

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceImpl
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Subsystem
import csw.prefix.models.Prefix

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object SequencerCommandServiceExample extends App {

  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem)

  // #create-sequencer-command-service
  private val connection             = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "sequencer"), ComponentType.Sequencer))
  private val location: AkkaLocation = Await.result(locationService.resolve(connection, 5.seconds), 5.seconds).get

  val sequencerCommandService: SequencerCommandService = new SequencerCommandServiceImpl(location)
  // #create-sequencer-command-service

  // #submit-sequence
  val sequence: Sequence        = Sequence(Setup(Prefix("test.move"), CommandName("command-1"), None))
  implicit val timeout: Timeout = Timeout(10.seconds)

  private val initialResponse: SubmitResponse = Await.result(sequencerCommandService.submit(sequence), 5.seconds)

  private val queryResponse: SubmitResponse = Await.result(sequencerCommandService.query(initialResponse.runId), 5.seconds)

  private val queryFinalResponse: SubmitResponse =
    Await.result(sequencerCommandService.queryFinal(initialResponse.runId), 5.seconds)
  // #submit-sequence

  // #submitAndWait
  private val finalResponse: SubmitResponse = Await.result(sequencerCommandService.submitAndWait(sequence), 5.seconds)
  // #submitAndWait

}
