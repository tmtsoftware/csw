package csw.framework.internal.component

import csw.framework.models.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage   extends DomainMessage
case class ComponentStats(value: Int) extends ComponentDomainMessage
