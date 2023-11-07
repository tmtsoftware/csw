/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.sequencerCommandService

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceImpl
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.{Prefix, Subsystem}

import cps.compat.FutureAsync.*
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationLong

object SequencerCommandServiceExample {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")
  implicit lazy val ec: ExecutionContextExecutor = typedSystem.executionContext
  private val locationService                    = HttpLocationServiceFactory.makeLocalClient(typedSystem)

  // #create-sequencer-command-service
  private val connection              = PekkoConnection(ComponentId(Prefix(Subsystem.CSW, "sequencer"), ComponentType.Sequencer))
  private val location: PekkoLocation = Await.result(locationService.resolve(connection, 5.seconds), 5.seconds).get

  val sequencerCommandService: SequencerCommandService = new SequencerCommandServiceImpl(location)
  // #create-sequencer-command-service

  // #submit-sequence
  val sequence: Sequence        = Sequence(Setup(Prefix("test.move"), CommandName("command-1"), None))
  implicit val timeout: Timeout = Timeout(10.seconds)
  def main(args: Array[String]): Unit = {
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
}
