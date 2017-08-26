package csw.common.framework.scaladsl.component

import csw.common.framework.models.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage   extends DomainMessage
case class ComponentStats(value: Int) extends ComponentDomainMessage
