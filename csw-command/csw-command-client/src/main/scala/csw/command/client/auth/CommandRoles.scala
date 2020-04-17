package csw.command.client.auth

import java.util

import com.typesafe.config.{Config, ConfigValue}
import csw.command.client.auth.CommandRoles.{AllowedRoles, CmdName}
import csw.prefix.models.Subsystem

import scala.jdk.CollectionConverters._

case class CommandRoles private (private val roles: Map[CmdName, AllowedRoles]) {
  def hasAccess(cmdName: CmdName, subsystem: Subsystem, userRoles: AllowedRoles): Boolean = {
    val _userRoles = userRoles.map(_.toLowerCase)
    val _cmdName   = cmdName.toLowerCase
    val _subsystem = subsystem.name.toLowerCase
    roles.get(_cmdName) match {
      case Some(allowedRoles) => allowedRoles.exists(_userRoles.contains)
      case None               => _userRoles.contains(_subsystem + "-user")
    }
  }
}

object CommandRoles {
  type CmdName      = String
  type AllowedRoles = Set[String]

  val empty: CommandRoles = CommandRoles(Map.empty)

  def from(config: Config): CommandRoles = {
    val entrySet = config.entrySet().asScala.toSet
    val cmdRoles = entrySet.map { entry: util.Map.Entry[String, ConfigValue] =>
      val cmdName = entry.getKey
      val roles   = config.getStringList(cmdName).asScala.toSet
      cmdName.toLowerCase -> roles.map(_.toLowerCase)
    }.toMap
    CommandRoles(cmdRoles)
  }
}
