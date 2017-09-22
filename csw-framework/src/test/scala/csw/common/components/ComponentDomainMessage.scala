package csw.common.components

import csw.framework.models.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage        extends DomainMessage
case class ComponentStatistics(value: Int) extends ComponentDomainMessage
