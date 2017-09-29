package csw.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.RunningMessage.DomainMessage
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.messages.{ComponentMessage, PubSub}
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.reflect.ClassTag

abstract class JComponentBehaviorFactory[Msg <: DomainMessage](
    klass: Class[Msg]
) extends ComponentBehaviorFactory[Msg]()(ClassTag(klass)) {

  protected[framework] def handlers(
      ctx: scaladsl.ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[Msg] =
    jHandlers(ctx.asJava, componentInfo, pubSubRef, locationService.asJava)

  protected[framework] def jHandlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: ILocationService
  ): JComponentHandlers[Msg]
}
