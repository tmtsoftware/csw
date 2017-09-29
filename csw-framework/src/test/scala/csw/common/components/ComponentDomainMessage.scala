package csw.common.components

import csw.messages.messages.RunningMessage.DomainMessage

sealed trait ComponentDomainMessage        extends DomainMessage
case class ComponentStatistics(value: Int) extends ComponentDomainMessage
