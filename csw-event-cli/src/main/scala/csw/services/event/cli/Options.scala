package csw.services.event.cli

import java.io.File

import csw.messages.events.EventKey

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

case class Options(
    cmd: String = "",
    eventKey: Option[EventKey] = None,
    eventKeys: Seq[EventKey] = Seq.empty,
    eventsMap: Map[EventKey, Set[String]] = Map.empty,
    eventData: File = new File("."),
    out: String = "oneline",
    printTimestamp: Boolean = false,
    printId: Boolean = false,
    printUnits: Boolean = false,
    maybeInterval: Option[FiniteDuration] = None,
    period: FiniteDuration = (Int.MaxValue / 1000).seconds
) {
  def isOneline: Boolean = out == "oneline"
  def isJson: Boolean    = out == "json"
}
