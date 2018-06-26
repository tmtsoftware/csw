package csw.services.event.cli

import csw.messages.events.EventKey

case class Options(
    op: String = "",
    eventKeys: Seq[EventKey] = Seq.empty
)
