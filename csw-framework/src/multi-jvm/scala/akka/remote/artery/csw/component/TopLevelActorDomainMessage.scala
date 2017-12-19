package akka.remote.artery.csw.component

import akka.actor.{ActorRef, DeadLetterSuppression}
import akka.remote.artery.RateReporter
import akka.testkit.JavaSerializable
import csw.messages.RunningMessage.DomainMessage

sealed trait TopLevelActorDomainMessage                 extends DomainMessage
final case class Start(correspondingReceiver: ActorRef) extends Echo with TopLevelActorDomainMessage

final case class InitComponentForPerfTest(reporter: RateReporter, payloadSize: Int, printTaskRunnerMetrics: Boolean)
    extends TopLevelActorDomainMessage

case object End                                             extends Echo with TopLevelActorDomainMessage
final case class Warmup(msg: AnyRef)                        extends TopLevelActorDomainMessage
final case class RawMessage(msg: AnyRef)                    extends TopLevelActorDomainMessage
final case class FlowControl(id: Int, burstStartTime: Long) extends Echo with TopLevelActorDomainMessage

object TestMessage {
  final case class Item(id: Long, name: String)
}

final case class TestMessage(
    id: Long,
    name: String,
    status: Boolean,
    description: String,
    payload: Array[Byte],
    items: Vector[TestMessage.Item]
) extends TopLevelActorDomainMessage

sealed trait Echo extends DeadLetterSuppression with JavaSerializable
