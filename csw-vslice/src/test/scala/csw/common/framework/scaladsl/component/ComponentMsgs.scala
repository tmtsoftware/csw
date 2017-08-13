package csw.common.framework.scaladsl.component

import csw.common.framework.models.RunningMsg.DomainMsg

sealed trait ComponentDomainMsg       extends DomainMsg
case class ComponentStats(value: Int) extends ComponentDomainMsg
