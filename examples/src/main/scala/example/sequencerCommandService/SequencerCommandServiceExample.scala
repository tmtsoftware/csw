package example.sequencerCommandService

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceImpl
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.{Prefix, Subsystem}
import example.sequencerCommandService.SequencerCommandServiceExample.sequencerCommandService

import scala.async.Async.{async, await}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationLong

object SequencerCommandServiceExample extends App {

  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")
  implicit lazy val ec: ExecutionContextExecutor = typedSystem.executionContext
  private val locationService                    = HttpLocationServiceFactory.makeLocalClient(typedSystem)

  // #create-sequencer-command-service
  private val connection             = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "sequencer"), ComponentType.Sequencer))
  private val location: AkkaLocation = Await.result(locationService.resolve(connection, 5.seconds), 5.seconds).get

  val sequencerCommandService: SequencerCommandService = new SequencerCommandServiceImpl(location)
  // #create-sequencer-command-service

  // #submit-sequence
  val sequence: Sequence        = Sequence(Setup(Prefix("test.move"), CommandName("command-1"), None))
  implicit val timeout: Timeout = Timeout(10.seconds)
  async {
    val initialResponse: SubmitResponse             = await(sequencerCommandService.submit(sequence))
    val queryResponseF: Future[SubmitResponse]      = sequencerCommandService.query(initialResponse.runId)
    val queryFinalResponseF: Future[SubmitResponse] = sequencerCommandService.queryFinal(initialResponse.runId)
    await(queryResponseF)
    await(queryFinalResponseF)
  }.map(_ => {
    // do something once all is finished
  })

  // #submit-sequence

  // #submitAndWait
  sequencerCommandService
    .submitAndWait(sequence)
    .map(finalResponse => {
      // do something with finalResponse
    })
  // #submitAndWait

}
