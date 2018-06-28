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

    cmd("inspect")
      .action((_, c) => c.copy(op = "inspect"))
      .text("returns event information excluding parameter values")
      .children(
        opt[Seq[String]]('e', "events")
          .required()
          .valueName("<event1>,<event2>...")
          .action((x, c) => c.copy(eventKeys = x.map(EventKey(_))))
          .text("comma separated list of events to inspect")
      )

    cmd("get")
      .action((_, c) => c.copy(op = "get"))
      .text("returns event")
      .children(
        opt[Seq[String]]('e', "events")
          .required()
          .valueName("<event1:key1>,<event2:key2:key3>...")
          .action { (x, c) =>
            val map = x.map { eventArg ⇒
              val events = eventArg.split(":").toSet
              EventKey(events.head) → events.tail
            }.toMap

            c.copy(eventsMap = map)
          }
          .text("comma separated list of <events:key-paths>"),
        opt[String]('o', "out")
          .valueName("oneline|json")
          .action((x, c) => c.copy(out = x))
          .text("output format, default is oneline"),
        opt[Unit]('t', "timestamp")
          .action((_, c) => c.copy(printTimestamp = true))
          .text("display timestamp"),
        opt[Unit]('i', "id")
          .action((_, c) => c.copy(printId = true))
          .text("display event id"),
        opt[Unit]('u', "units")
          .action((_, c) => c.copy(printUnits = true))
          .text("display units")
      )

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
