/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.auth

import java.util

import com.typesafe.config.{Config, ConfigValue}
import csw.aas.http.Roles
import csw.params.commands.CommandName
import csw.prefix.models.{Prefix, Subsystem}

import scala.jdk.CollectionConverters.*

// maps to key from command roles config file
case class CommandKey private (key: String) {
  private val subsystem = key.split('.').headOption.map(Subsystem.withNameInsensitive)
  require(subsystem.nonEmpty, s"$key should start with one of the valid subsystem")
}
object CommandKey {
  def apply(prefix: Prefix, commandName: CommandName): CommandKey =
    new CommandKey(prefix.toString.toLowerCase + "." + commandName.name.toLowerCase)

  def apply(key: String): CommandKey = new CommandKey(key.toLowerCase)
}

// maps to command roles config file
case class CommandRoles private[csw] (private[auth] val predefinedRoles: Map[CommandKey, Roles]) {
  def hasAccess(cmdKey: CommandKey, subsystem: Subsystem, rolesFromToken: Roles): Boolean = {
    def subsystemRoleNotPresentIn(allowedRoles: Roles): Boolean =
      !allowedRoles.containsAnyRole(subsystem)
    def tokenHasSubsystemUserRole: Boolean =
      rolesFromToken.containsUserRole(subsystem)

    def hasAccessToPredefinedCommands(allowedRoles: Roles, rolesFromToken: Roles): Boolean = {
      if (allowedRoles.intersect(rolesFromToken).nonEmpty) true
      else if (subsystemRoleNotPresentIn(allowedRoles))
        tokenHasSubsystemUserRole
      else false
    }

    predefinedRoles.get(cmdKey) match {
      case None => tokenHasSubsystemUserRole
      case Some(allowedRoles) =>
        hasAccessToPredefinedCommands(allowedRoles, rolesFromToken)
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
