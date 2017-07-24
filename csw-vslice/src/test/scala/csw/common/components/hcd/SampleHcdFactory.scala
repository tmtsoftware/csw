package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{HcdComponentLifecycleMessage, HcdMsg}
import csw.common.framework.scaladsl.{HcdActor, HcdActorFactory}

class SampleHcdFactory extends HcdActorFactory[HcdDomainMessage] {
  override def make(ctx: ActorContext[HcdMsg],
                    supervisor: ActorRef[HcdComponentLifecycleMessage]): HcdActor[HcdDomainMessage] =
    new SampleHcd(ctx, supervisor)
}
