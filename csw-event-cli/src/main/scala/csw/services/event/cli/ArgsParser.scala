package csw.services.event.cli

import csw.services.BuildInfo
import csw.messages.events.EventKey
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    cmd("inspect") action { (_, c) =>
      c.copy(op = "inspect")
    } text "returns info on an event" children {
      opt[Seq[String]]('e', "events")
        .valueName("<event1>,<event2>...")
        .action { (x, c) =>
          c.copy(eventKeys = x.map(EventKey(_)))
        }
        .text("list of events to inspect")
    }

    help("help")

    version("version")

    checkConfig { c =>
      if (c.op.isEmpty) failure("Please specify at least one command {inspect | ...}")
      else success
    }
  }

  /**
   * Parses the command line arguments and returns a value if they are valid.
   *
   * @param args the command line arguments
   * @return an object containing the parsed values of the command line arguments
   */
  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())

}
