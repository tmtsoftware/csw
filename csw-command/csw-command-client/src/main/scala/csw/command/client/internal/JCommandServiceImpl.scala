/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.internal

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import msocket.api.Subscription

import scala.jdk.FunctionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.CollectionConverters.*

private[command] class JCommandServiceImpl(commandService: CommandService) extends ICommandService {

  override def validate(controlCommand: ControlCommand): CompletableFuture[ValidateResponse] =
    commandService.validate(controlCommand).asJava.toCompletableFuture

  override def submit(controlCommand: ControlCommand): CompletableFuture[SubmitResponse] =
    commandService.submit(controlCommand).asJava.toCompletableFuture

  override def submitAndWait(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    commandService.submitAndWait(controlCommand)(timeout).asJava.toCompletableFuture

  override def submitAllAndWait(
      controlCommand: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]] =
    commandService
      .submitAllAndWait(controlCommand.asScala.toList)(timeout)
      .asJava
      .toCompletableFuture
      .thenApply(_.asJava)

  override def oneway(controlCommand: ControlCommand): CompletableFuture[OnewayResponse] =
    commandService.oneway(controlCommand).asJava.toCompletableFuture

  override def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): CompletableFuture[MatchingResponse] =
    commandService.onewayAndMatch(controlCommand, stateMatcher).asJava.toCompletableFuture

  override def query(commandRunId: Id): CompletableFuture[SubmitResponse] =
    commandService.query(commandRunId).asJava.toCompletableFuture

  override def queryFinal(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse] =
    commandService.queryFinal(commandRunId)(timeout).asJava.toCompletableFuture

  override def subscribeCurrentState(): Source[CurrentState, Subscription] =
    commandService.subscribeCurrentState().asJava

  override def subscribeCurrentState(names: java.util.Set[StateName]): Source[CurrentState, Subscription] =
    commandService.subscribeCurrentState(names.asScala.toSet).asJava

  override def subscribeCurrentState(callback: Consumer[CurrentState]): Subscription =
    commandService.subscribeCurrentState(callback.asScala)

  override def subscribeCurrentState(
      names: java.util.Set[StateName],
      callback: Consumer[CurrentState]
  ): Subscription = commandService.subscribeCurrentState(names.asScala.toSet, callback.asScala)
}
