package csw.services.event.cli

import java.io.File

import csw.messages.events.EventKey
import scopt.OptionParser

import scala.concurrent.duration.DurationDouble

trait CommandParams { self: OptionParser[Options] =>

  def eventkey =
    opt[String]('e', "event")
      .valueName("<event>")
      .action((x, c) => c.copy(eventKey = EventKey(x)))
      .text("event key to publish")

  def eventkeys =
    opt[Seq[String]]('e', "events")
      .required()
      .valueName("<event1>,<event2>...")
      .action((x, c) => c.copy(eventKeys = x.map(EventKey(_))))
      .text("comma separated list of events to inspect")

  def eventkeysWithPath =
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
      .text("comma separated list of <events:key-paths>")

  def interval =
    opt[Int]('i', "interval")
      .valueName("")
      .action((x, c) => c.copy(maybeInterval = Some(x.millis)))
      .validate { interval ⇒
        if (interval > 0) success
        else failure(s"invalid interval :$interval, should be > 0 milliseconds")
      }
      .text("interval in [ms] to publish event, single event will be published if not provided")

  def period =
    opt[Int]('p', "period")
      .action((x, c) => c.copy(period = x.seconds))
      .validate { period ⇒
        if (period > 0) success
        else failure(s"invalid period :$period, should be > 0 seconds")
      }
      .text(
        "publish events for this duration [seconds] on provided interval. Default is Int.MaxValue seconds"
      )

  def data =
    opt[File]("data")
      .valueName("<file>")
      .action((x, c) => c.copy(eventData = Some(x)))
      .validate(
        file ⇒
          if (file.exists()) success
          else failure(s"file [${file.getAbsolutePath}] does not exist")
      )
      .text("required: file path which contains event json")

  def params =
    opt[String]("params")
      .valueName("<k1:i:meter=10,20> <k2:s:volt=10v>")
      .action((x, c) => c.copy(params = ParameterArgParser.parse(x)))
      .text(
        """|space separated list of params in the form of "keyName:keyType:unit=values ...".
           |Imp: Multiple params should be provided in double quotes and unit is optional
           |""".stripMargin
      )

  def out =
    opt[String]('o', "out")
      .valueName("oneline|json")
      .action((x, c) => c.copy(out = x))
      .text("output format, default is oneline")

  def timestamp =
    opt[Unit]('t', "timestamp")
      .action((_, c) => c.copy(printTimestamp = true))
      .text("display timestamp")

  def id =
    opt[Unit]('i', "id")
      .action((_, c) => c.copy(printId = true))
      .text("display event id")

  def units =
    opt[Unit]('u', "units")
      .action((_, c) => c.copy(printUnits = true))
      .text("display units")
}
