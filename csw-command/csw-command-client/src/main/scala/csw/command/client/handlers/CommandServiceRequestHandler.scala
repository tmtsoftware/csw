/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.handlers

import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.command.api.codecs.CommandServiceCodecs.*
import csw.command.api.messages.CommandServiceRequest
import csw.command.api.messages.CommandServiceRequest.*
import csw.command.api.scaladsl.CommandService
import csw.command.client.auth.{CommandPolicy, CommandRoles}
import csw.params.commands.ControlCommand
import csw.prefix.models.Prefix
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

class CommandServiceRequestHandler(
    commandService: CommandService,
    securityDirectives: SecurityDirectives,
    destinationPrefix: Option[Prefix] = None,
    commandRoles: CommandRoles = CommandRoles.empty
) extends HttpPostHandler[CommandServiceRequest]
    with ServerHttpCodecs {

  override def handle(request: CommandServiceRequest): Route =
    request match {
      case Validate(controlCommand)               => sPost(controlCommand)(complete(commandService.validate(controlCommand)))
      case Submit(controlCommand)                 => sPost(controlCommand)(complete(commandService.submit(controlCommand)))
      case Oneway(controlCommand)                 => sPost(controlCommand)(complete(commandService.oneway(controlCommand)))
      case Query(runId)                           => complete(commandService.query(runId))
      case ExecuteDiagnosticMode(startTime, hint) => complete(commandService.executeDiagnosticMode(startTime, hint))
      case ExecuteOperationsMode()                => complete(commandService.executeOperationsMode())
      case GoOnline()                             => complete(commandService.onGoOnline())
      case GoOffline()                            => complete(commandService.onGoOffline())
    }

  private def sPost(controlCommand: ControlCommand)(route: => Route) =
    destinationPrefix match {
      case Some(prefix) => securityDirectives.sPost(CommandPolicy(commandRoles, controlCommand, prefix))(_ => route)
      case None         => route // auth is disabled in this case
    }
}
