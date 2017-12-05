package csw.common.components.framework

import csw.messages.RunningMessage.DomainMessage

sealed trait TopLevelActorDomainMessage        extends DomainMessage
case class TopLevelActorStatistics(value: Int) extends TopLevelActorDomainMessage
