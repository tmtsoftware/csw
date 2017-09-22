package csw.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.framework.models.RunningMessage.DomainMessage
import csw.framework.models.{ComponentInfo, ComponentMessage, PubSub}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.states.CurrentState
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.reflect.ClassTag

abstract class JComponentBehaviorFactory[Msg <: DomainMessage](
    klass: Class[Msg]
) extends ComponentBehaviorFactory[Msg]()(ClassTag(klass)) {

  def handlers(
      ctx: scaladsl.ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[Msg] =
    jHandlers(ctx.asJava, componentInfo, pubSubRef, locationService.asJava)

  def jHandlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: ILocationService
  ): JComponentHandlers[Msg]
}
