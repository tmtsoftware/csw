package csw.services.commons

import csw.messages.RunningMessage.DomainMessage

//#message-hierarchy
sealed trait ComponentDomainMessage        extends DomainMessage
case class ComponentStatistics(value: Int) extends ComponentDomainMessage
//#message-hierarchy
