package csw.command.api.utils

import com.typesafe.config.Config
import csw.command.api.utils.CommandRoleMapping.{AllowedCommands, Role}

import scala.jdk.CollectionConverters._

case class CommandRoleMapping(map: Map[Role, AllowedCommands])

object CommandRoleMapping {

  type Role            = String
  type AllowedCommands = List[String]

  def apply(config: Config): CommandRoleMapping = {
    val map = config
      .entrySet()
      .asScala
      .map { entry => entry.getKey -> config.getStringList(entry.getKey).asScala.toList }
      .toMap
    CommandRoleMapping(map)
  }


  val empty = new CommandRoleMapping(Map.empty)

}
