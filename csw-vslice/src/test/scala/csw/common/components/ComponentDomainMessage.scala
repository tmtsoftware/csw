package csw.common.components

import akka.typed.ActorRef
import csw.common.framework.models.RunningMessage.DomainMessage
import csw.param.states.CurrentState

sealed trait ComponentDomainMessage                         extends DomainMessage
case class ComponentStatistics(value: Int)                  extends ComponentDomainMessage
case class UpdateTestProbe(replyTo: ActorRef[CurrentState]) extends ComponentDomainMessage
