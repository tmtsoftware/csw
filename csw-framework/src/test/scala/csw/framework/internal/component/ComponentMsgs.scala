package csw.framework.internal.component

import csw.messages.messages.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage   extends DomainMessage
case class ComponentStats(value: Int) extends ComponentDomainMessage
