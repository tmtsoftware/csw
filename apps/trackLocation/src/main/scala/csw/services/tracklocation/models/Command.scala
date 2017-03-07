package csw.services.tracklocation.models

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigResolveOptions}
import csw.services.tracklocation.utils.{PortOptHandler, StringOptHandler}

case class Command(
    commandText: String,
    port: Int,
    delay: Int,
    noExit: Boolean
)

object Command{
  def parse(options:Options): Command = {
    val appConfig: Option[Config] = options.appConfigFile.flatMap(getAppConfig)
    val port = PortOptHandler("port", options.port, options, appConfig)
    val command = StringOptHandler("command", options.command, options, appConfig)
      .get
      .replace("%port", port.toString)
    val defaultDelay = 1000
    Command(command, port, options.delay.getOrElse(defaultDelay), options.noExit)
  }

  private def getAppConfig(file: File): Option[Config] = {
    if (file.exists()) Some(ConfigFactory.parseFileAnySyntax(file).resolve(ConfigResolveOptions.noSystem())) else None
  }
}


