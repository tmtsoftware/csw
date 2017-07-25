package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{HcdMsg, HcdResponseMode}
import csw.common.framework.scaladsl.hcd.{HcdActor, HcdActorFactory}

class SampleHcdFactory extends HcdActorFactory[HcdDomainMessage] {
  override def make(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]): HcdActor[HcdDomainMessage] =
    new SampleHcd(ctx, supervisor)
}

object SampleHcdFactory extends SampleHcdFactory
