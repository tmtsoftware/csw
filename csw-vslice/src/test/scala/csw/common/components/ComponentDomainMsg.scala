package csw.common.components

import csw.common.framework.models.RunningMsg.DomainMsg

sealed trait ComponentDomainMsg            extends DomainMsg
case class ComponentStatistics(value: Int) extends ComponentDomainMsg
