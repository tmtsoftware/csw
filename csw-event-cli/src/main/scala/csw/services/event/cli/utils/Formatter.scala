package csw.services.event.cli.utils

import csw.messages.events.{Event, EventKey}
import csw.messages.params.generics.Parameter
import csw.services.event.cli.args.Options

object Formatter {
  val eventSeparator =
    "==============================================================================================================="

  def invalidKey(eventKey: EventKey): String = s"$eventKey [ERROR] No events published for this key."
}

case class OnelineFormatter(options: Options) {

  def format(event: Event, lines: List[Oneline]): String = {
    lines
      .map { line =>
        if (options.printValues && options.printUnits) line.withValuesAndUnits()
        else if (options.printValues) line.withValues()
        else line.withKeyTypeAndUnits()
      }
      .sorted
      .mkString("\n")
  }

  def header(event: Event): String = {
    val timestamp = if (options.printTimestamp) s"Timestamp: ${event.eventTime.time.toString}" else ""
    val id        = if (options.printId) s"Id: ${event.eventId.id}" else ""
    val key       = s"EventKey: ${event.eventKey.key}"

    val header = List(key, timestamp, id).filter(_.nonEmpty).mkString(" | ")
    header + "\n"
  }
}

case class Oneline(path: String, param: Parameter[_]) {
  private val onelineSeparator = " = "

  private def values  = param.values.mkString("[", ",", "]")
  private def unitStr = List(param.units).mkString("[", "", "]")

  def withValues(): String = List(path, values).mkString(onelineSeparator)

  def withValuesAndUnits(): String = List(withValues(), unitStr).mkString(" ")

  def withKeyTypeAndUnits(): String = {
    val str = s"${param.keyType}$unitStr"
    List(path, str).mkString(onelineSeparator)
  }
}
