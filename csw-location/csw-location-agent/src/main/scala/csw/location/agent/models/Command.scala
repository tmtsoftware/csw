package csw.location.agent.models

import com.typesafe.config.Config
import csw.location.agent.args.Options
import csw.location.agent.utils.{OptionsHandler, Utils}

/**
 * Prepares a command object, based on in input parameters.
 *
 * @param commandText An executable command. e.g. "redis-server /usr/local/etc/redis.conf"
 * @param port Port number
 * @param delay Number of milliseconds
 * @param noExit prevents application from exiting after running the command
 */
case class Command(commandText: String, port: Int, delay: Int, noExit: Boolean, httpPath: Option[String])

object Command {
  val defaultDelay = 1000
  def parse(options: Options): Command = {
    val appConfig: Option[Config] = options.appConfigFile.flatMap(Utils.getAppConfig)
    val optionsHandler            = OptionsHandler(options, appConfig)
    val port                      = optionsHandler.portOpt("port", options.port)
    val command = optionsHandler
      .stringOpt("command", options.command)
      .getOrElse("false") //if command is not specified, registration will proceed with "false" command.
      .replace("%port", port.toString)
    Command(command, port, options.delay.getOrElse(defaultDelay), options.noExit, options.httpPath)
  }
}
