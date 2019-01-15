package csw.event.cli.utils

import csw.params.events.{Event, EventKey}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.event.cli.args.Options

object Formatter {
  val EventSeparator =
    "==============================================================================================================="

  def invalidKey(eventKey: EventKey): String = s"[ERROR] No events published for key: [$eventKey]"
}

case class OnelineFormatter(options: Options) {

  def format(lines: List[Oneline]): String =
    if (options.isOnelineOut) formatOnelineOutput(lines)
    else formatTerseOutput(lines)

  private def formatOnelineOutput(lines: List[Oneline]) =
    lines
      .map { line ⇒
        if (options.printValues && options.printUnits) line.withValuesAndUnits()
        else if (options.printValues) line.withValues()
        else line.withKeyTypeAndUnits()
      }
      .sorted
      .mkString("\n")

  private def formatTerseOutput(lines: List[Oneline]) = lines.map(_.terse).mkString("\n")

  def header(event: Event): String = {
    val timestamp = if (options.printTimestamp) s"Timestamp: ${event.eventTime.toString}" else ""
    val id        = if (options.printId) s"Id: ${event.eventId.id}" else ""
    val key       = s"EventKey: ${event.eventKey.key}"

    val header = List(key, timestamp, id).filter(_.nonEmpty).mkString(" | ")
    header + "\n"
  }
}

case class Oneline(path: String, param: Parameter[_]) {
  private val onelineSeparator = " = "

  private def values = {
    val paramValues = param.keyType match {
      case StringKey ⇒ param.values.map(v ⇒ s""""$v"""") // wrap string values in double quotes
      case _         ⇒ param.values
    }

    paramValues.mkString(", ")
  }

  private def unitStr = List(param.units).mkString("[", "", "]")

  def withValues(): String = List(path, values).mkString(onelineSeparator)

  def withValuesAndUnits(): String = List(withValues(), unitStr).mkString(" ")

  def withKeyTypeAndUnits(): String = {
    val str = s"${param.keyType}$unitStr"
    List(path, str).mkString(onelineSeparator)
  }

  def terse: String = values
}
