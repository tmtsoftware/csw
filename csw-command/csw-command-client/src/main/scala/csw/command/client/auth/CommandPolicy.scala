package csw.command.client.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.params.commands.ControlCommand
import csw.prefix.models.Subsystem

object CommandPolicy {
  def apply(roles: CommandRoles, command: ControlCommand, destinationSubsystem: Subsystem): CustomPolicy =
    CustomPolicy(token => roles.hasAccess(command.commandName.name, destinationSubsystem, token.realm_access.roles))
}
