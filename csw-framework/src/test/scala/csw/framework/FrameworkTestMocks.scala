package csw.framework

import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.supervisor.SupervisorBehavior
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.states.CurrentState
import csw.messages.{LifecycleStateChanged, PubSub, SupervisorMessage}
import csw.services.location.javadsl.ILocationService
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{ComponentLogger, Logger}
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
  val testActor: ActorRef[Any]                                      = testkit.TestProbe("test-probe").testActor
  val akkaRegistration                                              = AkkaRegistration(mock[AkkaConnection], Some("nfiraos.ncc.trombone"), testActor, testActor)
  val locationService: LocationService                              = mock[LocationService]
  val registrationResult: RegistrationResult                        = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory                      = mock[RegistrationFactory]
  val pubSubBehaviorFactory: PubSubBehaviorFactory                  = mock[PubSubBehaviorFactory]
  val lifecycleStateProbe: TestProbe[PubSub[LifecycleStateChanged]] = TestProbe[PubSub[LifecycleStateChanged]]
  val compStateProbe: TestProbe[PubSub[CurrentState]]               = TestProbe[PubSub[CurrentState]]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(Future.successful(registrationResult))
  when(locationService.unregister(any[AkkaConnection])).thenReturn(Future.successful(Done))
  when(locationService.asJava).thenReturn(mock[ILocationService])
  when(
    pubSubBehaviorFactory.make[LifecycleStateChanged](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubLifecycleActor),
      any[String]
    )
  ).thenReturn(lifecycleStateProbe.ref)
  when(
    pubSubBehaviorFactory.make[CurrentState](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubComponentActor),
      any[String]
    )
  ).thenReturn(compStateProbe.ref)

}

object FrameworkTestMocks {
  trait MutableActorMock[T] { this: ComponentLogger.MutableActor[T] ⇒
    override lazy val log: Logger = MockitoSugar.mock[Logger]
  }
}
