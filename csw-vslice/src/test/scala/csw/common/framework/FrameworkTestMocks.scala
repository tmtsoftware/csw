package csw.common.framework

import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.internal.supervisor.SupervisorBehavior
import csw.common.framework.models.{LifecycleStateChanged, PubSub, SupervisorMessage}
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class FrameworkTestMocks(
    implicit untypedSystem: actor.ActorSystem,
    system: ActorSystem[Nothing],
    settings: TestKitSettings
) extends MockitoSugar {
  val akkaRegistration                                              = AkkaRegistration(mock[AkkaConnection], testkit.TestProbe("test-probe").testActor)
  val locationService: LocationService                              = mock[LocationService]
  val registrationResult: RegistrationResult                        = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory                      = mock[RegistrationFactory]
  val pubSubBehaviorFactory: PubSubBehaviorFactory                  = mock[PubSubBehaviorFactory]
  val lifecycleStateProbe: TestProbe[PubSub[LifecycleStateChanged]] = TestProbe[PubSub[LifecycleStateChanged]]
  val compStateProbe: TestProbe[PubSub[CurrentState]]               = TestProbe[PubSub[CurrentState]]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[ActorRef[_]])).thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(Future.successful(registrationResult))
  when(registrationResult.unregister()).thenReturn(Future.successful(Done))
  when(
    pubSubBehaviorFactory.make[LifecycleStateChanged](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubLifecycleActor)
    )
  ).thenReturn(lifecycleStateProbe.ref)
  when(
    pubSubBehaviorFactory.make[CurrentState](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubComponentActor)
    )
  ).thenReturn(compStateProbe.ref)

}
