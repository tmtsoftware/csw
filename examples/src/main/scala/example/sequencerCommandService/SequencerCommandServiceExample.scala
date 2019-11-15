package example.sequencerCommandService

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import csw.command.client.SequencerCommandServiceFactory
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Prefix, Subsystem}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object SequencerCommandServiceExample extends App {

  implicit val mat: Materializer = Materializer(typedSystem)
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private val locationService = HttpLocationServiceFactory.makeLocalClient(typedSystem, mat)

  // #create-sequence-command-service
  private val connection             = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "sequencer"), ComponentType.Sequencer))
  private val location: AkkaLocation = Await.result(locationService.resolve(connection, 5.seconds), 5.seconds).get

  val sequencerCommandService: SequencerCommandServiceImpl = SequencerCommandServiceFactory.make(location)
  // #create-sequence-command-service

  // #submit-sequence
  val sequence: Sequence = Sequence(Setup(Prefix("test.move"), CommandName("command-1"), None))

  private val submitResponse: SubmitResponse = Await.result(sequencerCommandService.submitAndWait(sequence), 5.seconds)
  // #submit-sequence

}
