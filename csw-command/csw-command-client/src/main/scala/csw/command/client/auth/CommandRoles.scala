package csw.command.client.auth

import java.util

import com.typesafe.config.{Config, ConfigValue}
import csw.aas.core.token.AccessToken
import csw.params.commands.CommandName
import csw.prefix.models.{Prefix, Subsystem}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

// maps to key from command roles config file
case class CommandKey @nowarn private (key: String) {
  private val subsystem = key.split('.').headOption.map(Subsystem.withNameInsensitive)
  require(subsystem.nonEmpty, s"$key should start with one of the valid subsystem")
}
object CommandKey {
  def apply(prefix: Prefix, commandName: CommandName): CommandKey =
    new CommandKey(prefix.toString.toLowerCase + "." + commandName.name.toLowerCase)

  def apply(key: String): CommandKey = new CommandKey(key.toLowerCase)
}

// maps to all the associated roles for the key from command roles config file
case class Roles @nowarn private (roles: Set[String]) {
  def exist(that: Roles): Boolean                     = this.roles.exists(that.roles.contains)
  def containsUserRole(subsystem: Subsystem): Boolean = roles.contains(subsystem.name.toLowerCase + "-user")
  def containsAnyRole(subsystem: Subsystem): Boolean  = roles.exists(_.contains(subsystem.name.toLowerCase))
}
object Roles {
  def apply(roles: Set[String]): Roles = new Roles(roles.map(_.toLowerCase))
  def apply(token: AccessToken): Roles = Roles(token.realm_access.roles)
}

// maps to command roles config file
case class CommandRoles @nowarn private (private[auth] val predefinedRoles: Map[CommandKey, Roles]) {
  def hasAccess(cmdKey: CommandKey, subsystem: Subsystem, rolesFromToken: Roles): Boolean = {

    val commandFound: Boolean             = predefinedRoles.contains(cmdKey)
    val roleFound: Boolean                = predefinedRoles.get(cmdKey).exists(_.exist(rolesFromToken))
    val anyRoleForSubsystemFound: Boolean = predefinedRoles.get(cmdKey).exists(_.containsAnyRole(subsystem))
    val userHasSubsystemUserRole: Boolean = rolesFromToken.containsUserRole(subsystem)

    CommandPolicy(commandFound, roleFound, anyRoleForSubsystemFound, userHasSubsystemUserRole) match {
      case CommandPolicy(true, true, _, _)     => true
      case CommandPolicy(true, _, false, true) => true
      case CommandPolicy(false, _, _, true)    => true
      case _                                   => false
    }
  }
}

object CommandRoles {
  val empty: CommandRoles = CommandRoles(Map.empty)

  def from(config: Config): CommandRoles = {
    val entrySet = config.entrySet().asScala.toSet
    val cmdRoles = entrySet.map { entry: util.Map.Entry[String, ConfigValue] =>
      val cmdKey = entry.getKey
      val roles  = config.getStringList(cmdKey).asScala.toSet
      CommandKey(cmdKey) -> Roles(roles)
    }.toMap
    CommandRoles(cmdRoles)
  }
}
