package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.typed.ActorRef
import akka.util.Timeout
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, ControlCommand}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

case class JCommandDistributor(componentToCommands: java.util.Map[ActorRef[ComponentMessage], Set[ControlCommand]]) {

  def execute()(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val commandResponsesF: Source[CommandResponse, NotUsed] = Source(componentToCommands.asScala.toMap).flatMapMerge(
      10,
      { case (component, commands) ⇒ ComponentRef(component).submitAllAndSubscribe(commands) }
    )
    CommandResponse.aggregateResponse(commandResponsesF).toJava.toCompletableFuture
  }
}
