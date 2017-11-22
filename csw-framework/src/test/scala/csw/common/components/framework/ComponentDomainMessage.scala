package csw.common.components.framework

import csw.messages.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage        extends DomainMessage
case class ComponentStatistics(value: Int) extends ComponentDomainMessage
