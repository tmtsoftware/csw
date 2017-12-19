package csw.services.ccs.scaladsl

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, ControlCommand}

import scala.concurrent.{ExecutionContext, Future}

case class CommandDistributor(componentToCommands: Map[ComponentRef, Set[ControlCommand]]) {

  def submitAll()(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[CommandResponse] = {

    val commandResponsesF: Source[CommandResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      10,
      { case (component, commands) ⇒ component.submitAll(commands) }
    )
    CommandResponse.aggregateResponse(commandResponsesF)
  }

  def submitAllAndSubscribe()(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[CommandResponse] = {

    val commandResponsesF: Source[CommandResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      10,
      { case (component, commands) ⇒ component.submitAllAndSubscribe(commands) }
    )
    CommandResponse.aggregateResponse(commandResponsesF)
  }
}
