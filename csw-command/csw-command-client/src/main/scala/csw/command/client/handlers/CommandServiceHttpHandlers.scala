package csw.command.client.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, RealmRolePolicy}
import csw.aas.http.SecurityDirectives
import csw.command.api.codecs.CommandServiceCodecs._
import csw.command.api.messages.CommandServiceHttpMessage
import csw.command.api.messages.CommandServiceHttpMessage._
import csw.command.api.scaladsl.CommandService
import csw.command.api.utils.CommandRoleMapping
import csw.command.api.utils.CommandRoleMapping.{AllowedCommands, Role}
import csw.prefix.models.Prefix
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class CommandServiceHttpHandlers(
    commandService: CommandService,
    securityDirectives: SecurityDirectives,
    destinationPrefix: Option[Prefix] = None,
    commandRoleMapping: CommandRoleMapping = CommandRoleMapping.empty
) extends HttpPostHandler[CommandServiceHttpMessage]
    with ServerHttpCodecs {

  override def handle(request: CommandServiceHttpMessage): Route = request match {
    case Validate(controlCommand) =>
      val commandNameWithSubsystem = destinationPrefix.get.subsystem + controlCommand.commandName.name
      securityDirectives.sPost(commandPolicy(commandNameWithSubsystem))(_ => complete(commandService.validate(controlCommand)))
    case Submit(controlCommand) =>
      val commandNameWithSubsystem = destinationPrefix.get.subsystem + controlCommand.commandName.name
      securityDirectives.sPost(commandPolicy(commandNameWithSubsystem))(_ => complete(commandService.submit(controlCommand)))
    case Oneway(controlCommand) =>
      val commandNameWithSubsystem = destinationPrefix.get.subsystem + controlCommand.commandName.name
      securityDirectives.sPost(commandPolicy(commandNameWithSubsystem))(_ => complete(commandService.oneway(controlCommand)))
    case Query(runId) => complete(commandService.query(runId))
  }

  private def commandPolicy(commandName: String): CustomPolicy = {
    CustomPolicy { token =>
      commandRoleMapping.map.exists { entry: (Role, AllowedCommands) =>
        token.realm_access.roles.contains(entry._1) && entry._2.contains(commandName)
      }
    }
  }

  //  private def commandPermissionForSubsystem(subsystem: String, commandName: String): CustomPolicy = {
  //    CustomPolicy { token =>
  //      commandRoleMapping.get.map
  //        .filter { role => role._1.contains(subsystem) } // APS-user APS-eng APS-admin
  //        .exists { entry: (Role, AllowedCommands) =>
  //          token.realm_access.roles.contains(entry._1) && entry._2.contains(commandName)
  //        }
  //    }
  //  }

  //  private def rolePermissionWithoutSubsystemCheck(role: String): CustomPolicy = {
  //    CustomPolicy { token => token.realm_access.roles.exists { roleFromToken => roleFromToken.contains(role) } }
  //  }

}
