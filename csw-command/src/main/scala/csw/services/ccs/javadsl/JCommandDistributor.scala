package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.util.Timeout
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, ControlCommand, JComponentRef}
import csw.services.ccs.scaladsl.CommandDistributor

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

case class JCommandDistributor(componentToCommands: java.util.Map[JComponentRef, Set[ControlCommand]]) {

  def submitAll(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ ComponentRef(component.value) -> commands
    }
    CommandDistributor(sComponentToCommands).submitAll()(timeout, scheduler, ec, mat).toJava.toCompletableFuture
  }

  def submitAllAndSubscribe(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ ComponentRef(component.value) -> commands
    }
    CommandDistributor(sComponentToCommands).submitAllAndSubscribe()(timeout, scheduler, ec, mat).toJava.toCompletableFuture
  }
}
