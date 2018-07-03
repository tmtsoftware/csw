package csw.services.event.cli

import java.io.File

import csw.messages.events.EventKey
import csw.services.BuildInfo
import scopt.OptionParser

import scala.concurrent.duration.DurationLong

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    cmd("inspect")
      .action((_, c) => c.copy(cmd = "inspect"))
      .text("returns event information excluding parameter values")
      .children(
        opt[Seq[String]]('e', "events")
          .required()
          .valueName("<event1>,<event2>...")
          .action((x, c) => c.copy(eventKeys = x.map(EventKey(_))))
          .text("comma separated list of events to inspect")
      )

    cmd("get")
      .action((_, c) => c.copy(cmd = "get"))
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

    cmd("publish")
      .action((_, c) => c.copy(cmd = "publish"))
      .text("publishes event provided from input file")
      .children(
        opt[String]('e', "event")
          .required()
          .valueName("<event>")
          .action((x, c) => c.copy(eventKey = Some(EventKey(x))))
          .text("required: event to publish"),
        opt[File]("data")
          .required()
          .valueName("<file>")
          .action((x, c) => c.copy(eventData = x))
          .validate(
            file ⇒
              if (file.exists()) success
              else failure(s"file [${file.getAbsolutePath}] does not exist")
          )
          .text("required: file path which contains event json"),
        opt[Int]('i', "interval")
          .action((x, c) => c.copy(interval = Some(x.millis)))
          .validate { interval ⇒
            if (interval > 0) success
            else failure(s"invalid interval :$interval, should be > 0 milliseconds")
          }
          .text("interval in [ms] to publish event, single event will be published if not provided"),
        opt[Int]('d', "duration")
          .action((x, c) => c.copy(duration = x.seconds))
          .validate { duration ⇒
            if (duration > 0) success
            else failure(s"invalid duration :$duration, should be > 0 seconds")
          }
          .text(
            "publish events for this duration [seconds] on provided interval. Default is Int.MaxValue seconds"
          )
      )

    help("help")

    version("version")

    checkConfig { c =>
      if (c.cmd.isEmpty)
        failure("""
          |Please specify one of the following command with their corresponding options:
          |  1> Inspect
          |  2> Get
          |  3> Subscribe
          |  4> Publish
        """.stripMargin)
      else success
    }
  }

  /**®
   * Parses the command line arguments and returns a value if they are valid.
   *
   * @param args the command line arguments
   * @return an object containing the parsed values of the command line arguments
   */
  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())

}
