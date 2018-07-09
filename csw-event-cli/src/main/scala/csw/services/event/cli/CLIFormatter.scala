package csw.services.event.cli

import csw.messages.events.{Event, EventKey}
import csw.messages.params.generics.Parameter
import ujson.Js
import upickle.default.write

abstract class CLIFormatter(options: Options) {

  val eventSeparator = "========================================================================="

  def header(event: Event): String = {
    val timestamp = if (options.printTimestamp) event.eventTime.time.toString else ""
    val id        = if (options.printId) event.eventId.id else ""
    val header    = List(timestamp, id, event.eventKey.key).filter(_.nonEmpty).mkString(" ")
    header + "\n"
  }

  def invalidKey(eventKey: EventKey): String = s"$eventKey [ERROR] No events published for this key."

}

case class JsonFormatter(options: Options) extends CLIFormatter(options) {
  def format(eventJson: Js.Obj): String = write(eventJson, 4)
}

case class OnelineFormatter(options: Options) extends CLIFormatter(options) {
  def format(event: Event, onelines: List[Oneline]): String =
    EventOutput(event, options, onelines)
      .onelines()
      .sorted
      .mkString("\n")
}

case class EventOutput(event: Event, options: Options, lines: List[Oneline]) {
  def onelines(): List[String] =
    lines
      .map(
        line ⇒
          if (options.cmd == "get")
            if (options.printUnits) line.withValuesAndUnits() else line.withValues()
          else line.withKeyTypeAndUnits()
      )
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
