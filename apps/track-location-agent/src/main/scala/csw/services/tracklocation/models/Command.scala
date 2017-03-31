package csw.services.tracklocation.models

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigResolveOptions}
import csw.services.tracklocation.utils.{OptionsHandler, Utils}

case class Command(
    commandText: String,
    port: Int,
    delay: Int,
    noExit: Boolean
)

object Command{
  val defaultDelay = 1000
  def parse(options:Options): Command = {
    val appConfig: Option[Config] = options.appConfigFile.flatMap(Utils.getAppConfig)
    val optionsHandler = OptionsHandler(options, appConfig)
    val port = optionsHandler.portOpt("port", options.port)
    val command = optionsHandler.stringOpt("command", options.command)
      .getOrElse("false") //if command is not specified, registration will proceed with "false" command.
      .replace("%port", port.toString)
    Command(command, port, options.delay.getOrElse(defaultDelay), options.noExit)
  }
}