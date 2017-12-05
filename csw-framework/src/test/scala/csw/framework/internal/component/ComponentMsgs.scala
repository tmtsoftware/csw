package csw.framework.internal.component

import csw.messages.RunningMessage.DomainMessage

sealed trait TopLevelActorDomainMessage   extends DomainMessage
case class TopLevelActorStats(value: Int) extends TopLevelActorDomainMessage
