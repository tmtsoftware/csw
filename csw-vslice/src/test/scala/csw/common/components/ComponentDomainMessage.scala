package csw.common.components

import csw.common.framework.models.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage        extends DomainMessage
case class ComponentStatistics(value: Int) extends ComponentDomainMessage
