package csw.location.agent.args
import java.io.File

/**
 * Command line options ("csw-location-agent --help" prints a usage message with descriptions of all the options)
 *
 * @param names         names is a comma separated list of services (without whitespace or hyphen) to be registered with
 *                      LocationService. e.g. "Alarm,Telemetry,Configuration"
 * @param command       An executable command. e.g. "redis-server /usr/local/etc/redis.conf"
 * @param port          Optional port number the application listens on
 * @param appConfigFile Optional config file in HOCON format
 * @param delay         Number of milliseconds to wait for the app to start
 * @param noExit        For testing, prevents application from exiting after running the command
 */
case class Options(
    names: List[String] = Nil,
    command: Option[String] = None,
    port: Option[Int] = None,
    appConfigFile: Option[File] = None,
    delay: Option[Int] = None,
    noExit: Boolean = false
)
