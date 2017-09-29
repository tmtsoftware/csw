package csw.framework.internal.component

import csw.messages.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage   extends DomainMessage
case class ComponentStats(value: Int) extends ComponentDomainMessage
