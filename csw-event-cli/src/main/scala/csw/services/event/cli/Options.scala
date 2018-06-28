package csw.services.event.cli

import csw.messages.events.EventKey

case class Options(
    op: String = "",
    eventKeys: Seq[EventKey] = Seq.empty,
    eventsMap: Map[EventKey, Set[String]] = Map.empty,
    out: String = "oneline",
    printTimestamp: Boolean = false,
    printId: Boolean = false,
    printUnits: Boolean = false
)
