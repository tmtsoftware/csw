package csw.services.tracklocation.utils

import java.io.File

import csw.services.tracklocation.models.Options
import scopt.OptionParser

object CmdLineArgsParser {
  // Parses the command line options
  val parser: OptionParser[Options] = new scopt.OptionParser[Options]("trackLocation") {
    head("trackLocation", System.getProperty("CSW_VERSION"))

    opt[Seq[String]]("name")
      .required()
      .valueName("<name1>[,<name2>,...]")
      .action ( (x, c) =>
        c.copy(names = x.toList)
      )
      .validate(x =>
        if(x.exists(s => !s.contains("-") && s == s.trim)) { success }
        else{failure("Service name cannot have '-' or leading/trailing spaces") })
      .text("Required: The name (or names, separated by comma) used to register the application (also root name in config file). The names should not have 1) leading and/or trailing spaces 2) '-'")

    opt[String]('c', "command") valueName "<name>"action { (x, c) =>
      c.copy(command = Some(x))
    } text "The command that starts the target application: use %port to insert the port number (default: use $name.command from config file: Required). The command is optional. If not provided, the service names provided will be registered via Location Service."

    opt[Int]('p', "port") valueName "<number>" action { (x, c) =>
      c.copy(port = Some(x))
    } text "Optional port number the application listens on (default: use value of $name.port from config file, or use a random, free port.)"

    arg[File]("<app-config>") optional () maxOccurs 1 action { (x, c) =>
      c.copy(appConfigFile = Some(x))
    } text "optional config file in HOCON format (Options specified as: $name.command, $name.port, etc. Fetched from config service if path does not exist)"

    opt[Int]("delay") action { (x, c) =>
      c.copy(delay = Some(x))
    } text "number of milliseconds to wait for the app to start before registering it with the location service (default: 1000)"

    opt[Unit]("no-exit") action { (_, c) =>
      c.copy(noExit = true)
    } text "for testing: prevents application from exiting after running command"

    help("help")
    version("version")

    //override def terminate(exitState: Either[String, Unit]): Unit = ()
  }
}

