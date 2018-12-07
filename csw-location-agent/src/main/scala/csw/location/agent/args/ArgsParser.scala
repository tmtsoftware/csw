package csw.location.agent.args

import java.io.File

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    def acceptableServiceNames(services: Seq[String]): Either[String, Unit] = {
      val allValid = services.forall { service =>
        !service.contains("-") && service.trim == service
      }
      if (allValid) success
      else failure("Service name cannot have '-' or leading/trailing spaces")
    }

    opt[Seq[String]]("name")
      .required()
      .valueName("<name1>[,<name2>,...]")
      .action((x, c) => c.copy(names = x.toList))
      .validate(xs => acceptableServiceNames(xs))
      .text(
        "Required: The name (or names, separated by comma) used to register the application (also root name in config file)."
      )

    opt[String]('c', "command") valueName "<name>" action { (x, c) =>
      c.copy(command = Some(x))
    } text "The parameter is optional. The command that starts the target application. Use use %port to specify the port number. If parameter is not provided value $name.command from config file will be picked up. If value in config file is not found, the service names provided will be registered with Location Service."

    opt[Int]('p', "port") valueName "<number>" action { (x, c) =>
      c.copy(port = Some(x))
    } text "Optional port number the application listens on (default: use value of $name.port from config file, or use a random, free port.)"

    arg[File]("<app-config>") optional () maxOccurs 1 action { (x, c) =>
      c.copy(appConfigFile = Some(x))
    } text "optional config file in HOCON format (Options specified as: $name.command, $name.port, etc.)"

    opt[Int]("delay") action { (x, c) =>
      c.copy(delay = Some(x))
    } text "number of milliseconds to wait for the app to start before registering it with the location service (default: 1000)"

    opt[Unit]("no-exit") action { (_, c) =>
      c.copy(noExit = true)
    } text "for testing: prevents application from exiting after running command"

    opt[Unit]("http") action { (_, c) ⇒
      c.copy(asHttp = true)
    } text "flag to register services as Http, by default services will be registered as tcp "

    help("help")
    version("version")
  }

  /**
   * Parses the command line arguments and returns a value if they are valid.
   *
   * @param args the command line arguments
   * @return an object containing the parsed values of the command line arguments
   */
  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())

}
