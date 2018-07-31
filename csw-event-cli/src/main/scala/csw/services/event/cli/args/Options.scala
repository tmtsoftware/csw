package csw.services.event.cli.args

import java.io.File

import csw.messages.events.EventKey
import csw.messages.params.generics.Parameter

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

case class Options(
    cmd: String = "",
    eventKey: EventKey = EventKey("unused.key"),
    eventKeys: Seq[EventKey] = Seq.empty,
    eventsMap: Map[EventKey, Set[String]] = Map.empty,
    eventData: Option[File] = None,
    params: Set[Parameter[_]] = Set.empty,
    out: String = "oneline",
    printTimestamp: Boolean = false,
    printId: Boolean = false,
    printUnits: Boolean = false,
    maybeInterval: Option[FiniteDuration] = None,
    period: FiniteDuration = (Int.MaxValue / 1000).seconds,
) {
  def isJsonOut: Boolean                 = out == "json"
  def isOnelineOut: Boolean              = out == "oneline"
  def isTerseOut: Boolean                = out == "terse"
  def printValues: Boolean               = cmd != "inspect"
  def paths(key: EventKey): List[String] = eventsMap.getOrElse(key, Nil).toList
}
