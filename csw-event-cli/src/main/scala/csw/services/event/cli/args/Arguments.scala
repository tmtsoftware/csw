package csw.services.event.cli.args

import java.io.File

import csw.messages.events.EventKey
import scopt.{OptionDef, OptionParser}

import scala.concurrent.duration.DurationDouble

trait Arguments { self: OptionParser[Options] =>

  def eventkey: OptionDef[String, Options] =
    opt[String]('e', "event")
      .required()
      .valueName("<event>")
      .action((x, c) => c.copy(eventKey = EventKey(x)))
      .text("required: event key to publish")

  def eventkeys: OptionDef[Seq[String], Options] =
    opt[Seq[String]]('e', "events")
      .required()
      .valueName("<event1>,<event2>...")
      .action((x, c) => c.copy(eventKeys = x.map(EventKey(_))))
      .text("required: comma separated list of events to inspect")

  def eventkeysWithPath: OptionDef[Seq[String], Options] =
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
      .text("required: comma separated list of <events:key-paths>")

  def data: OptionDef[File, Options] =
    opt[File]("data")
      .valueName("<file>")
      .action((x, c) => c.copy(eventData = Some(x)))
      .validate(
        file ⇒
          if (file.exists()) success
          else failure(s"file [${file.getAbsolutePath}] does not exist")
      )
      .text("file path which contains event json")

  def params: OptionDef[String, Options] =
    opt[String]("params")
      .valueName("<k1:i:meter=10,20> <k2:s:volt=10v>")
      .action((x, c) => c.copy(params = ParameterArgParser.parse(x)))
      .text(
        """|space separated list of params in the form of "keyName:keyType:unit=values ...".
           |Imp: Multiple params should be provided in double quotes and unit is optional
           |""".stripMargin
      )

  def interval: OptionDef[Int, Options] =
    opt[Int]('i', "interval")
      .action((x, c) => c.copy(maybeInterval = Some(x.millis)))
      .validate { interval ⇒
        if (interval > 0) success
        else failure(s"invalid interval :$interval, should be > 0 milliseconds")
      }

  def period: OptionDef[Int, Options] =
    opt[Int]('p', "period")
      .action((x, c) => c.copy(period = x.seconds))
      .validate { period ⇒
        if (period > 0) success
        else failure(s"invalid period :$period, should be > 0 seconds")
      }
      .text(
        s"publish events for this duration [seconds] on provided interval. Default is ${Int.MaxValue / 1000} seconds"
      )

  def out: OptionDef[String, Options] =
    opt[String]('o', "out")
      .valueName("oneline|json")
      .action((x, c) => c.copy(out = x))
      .text("output format, default is oneline")

  def timestamp: OptionDef[Unit, Options] =
    opt[Unit]('t', "timestamp")
      .action((_, c) => c.copy(printTimestamp = true))
      .text("display timestamp")

  def id: OptionDef[Unit, Options] =
    opt[Unit]("id")
      .action((_, c) => c.copy(printId = true))
      .text("display event id")

  def units: OptionDef[Unit, Options] =
    opt[Unit]('u', "units")
      .action((_, c) => c.copy(printUnits = true))
      .text("display units")
}
