package csw.common.framework

import akka.typed.scaladsl.adapter._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.{actor, testkit, Done}
import csw.common.components.ComponentDomainMessage
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.internal.supervisor.SupervisorBehavior
import csw.common.framework.models.PubSub.PublisherMessage
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future, Promise}

class TestMocks(
    implicit untypedSystem: actor.ActorSystem,
    system: ActorSystem[Nothing],
    settings: TestKitSettings
) extends MockitoSugar {
  val akkaRegistration                                              = AkkaRegistration(mock[AkkaConnection], testkit.TestProbe("test-probe").testActor)
  val locationService: LocationService                              = mock[LocationService]
  val registrationResult: RegistrationResult                        = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory                      = mock[RegistrationFactory]
  val eventualDone: Future[Done]                                    = Promise[Done].success(Done).future
  val pubSubBehaviorFactory: PubSubBehaviorFactory                  = mock[PubSubBehaviorFactory]
  val lifecycleStateProbe: TestProbe[PubSub[LifecycleStateChanged]] = TestProbe[PubSub[LifecycleStateChanged]]
  val compStateProbe: TestProbe[PubSub[CurrentState]]               = TestProbe[PubSub[CurrentState]]
  val eventualRegistrationResult: Future[RegistrationResult] =
    Promise[RegistrationResult].success(registrationResult).future

  when(registrationFactory.akkaTyped(ArgumentMatchers.any[AkkaConnection], ArgumentMatchers.any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(eventualRegistrationResult)
  when(registrationResult.unregister()).thenReturn(eventualDone)
  when(
    pubSubBehaviorFactory.make[LifecycleStateChanged](
      ArgumentMatchers.any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubLifecycleActor)
    )
  ).thenReturn(lifecycleStateProbe.ref)
  when(
    pubSubBehaviorFactory.make[CurrentState](
      ArgumentMatchers.any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubComponentActor)
    )
  ).thenReturn(compStateProbe.ref)

}

abstract class FrameworkTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val untypedSystem: actor.ActorSystem = ActorSystemFactory.remote()
  implicit val system: ActorSystem[Nothing]     = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings        = TestKitSettings(system)
  implicit val timeout: Timeout                 = Timeout(5.seconds)
  def testMocks: TestMocks                      = new TestMocks

  override protected def afterAll(): Unit = {
    Await.result(untypedSystem.terminate(), 5.seconds)
    Await.result(system.terminate(), 5.seconds)
  }

  def getSampleHcdWiring(
      componentHandlers: ComponentHandlers[ComponentDomainMessage]
  ): ComponentBehaviorFactory[ComponentDomainMessage] =
    new ComponentBehaviorFactory[ComponentDomainMessage] {

      override def handlers(
          ctx: ActorContext[ComponentMessage],
          componentInfo: ComponentInfo,
          pubSubRef: ActorRef[PublisherMessage[CurrentState]]
      ): ComponentHandlers[ComponentDomainMessage] =
        componentHandlers
    }

  def getSampleAssemblyWiring(
      assemblyHandlers: ComponentHandlers[ComponentDomainMessage]
  ): ComponentBehaviorFactory[ComponentDomainMessage] =
    new ComponentBehaviorFactory[ComponentDomainMessage] {
      override def handlers(
          ctx: ActorContext[ComponentMessage],
          componentInfo: ComponentInfo,
          pubSubRef: ActorRef[PublisherMessage[CurrentState]]
      ): ComponentHandlers[ComponentDomainMessage] =
        assemblyHandlers
    }

}
