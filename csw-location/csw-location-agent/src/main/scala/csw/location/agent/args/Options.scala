package csw.location.agent.args
import java.io.File

import csw.prefix.models.Prefix

/**
 * Command line options ("csw-location-agent --help" prints a usage message with descriptions of all the options)
 *
 * @param prefixes      prefixes is a comma separated list of services (without whitespace or hyphen) to be registered with
 *                      LocationService. e.g. "CSW.Alarm,CSW.Telemetry,CSW.Configuration"
 * @param command       An executable command. e.g. "redis-server /usr/local/etc/redis.conf"
 * @param port          Optional port number the application listens on
 * @param appConfigFile Optional config file in HOCON format
 * @param delay         Number of milliseconds to wait for the app to start
 * @param noExit        For testing, prevents application from exiting after running the command
 * @param httpPath      For registering services as Http location with the given path
 */
case class Options(
    prefixes: List[Prefix] = Nil,
    command: Option[String] = None,
    port: Option[Int] = None,
    appConfigFile: Option[File] = None,
    delay: Option[Int] = None,
    noExit: Boolean = false,
    httpPath: Option[String] = None
)
