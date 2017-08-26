package csw.common.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.{actor, testkit, Done}
import csw.common.components.ComponentDomainMsg
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.{ComponentInfo, ComponentMsg}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future, Promise}

abstract class FrameworkComponentTestSuite extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {
  implicit val untypedSystem: actor.ActorSystem          = ActorSystemFactory.remote()
  implicit val system: ActorSystem[Nothing]              = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings                 = TestKitSettings(system)
  implicit val timeout: Timeout                          = Timeout(5.seconds)
  protected val akkaRegistration                         = AkkaRegistration(mock[AkkaConnection], testkit.TestProbe("test-probe").testActor)
  protected val locationService: LocationService         = mock[LocationService]
  protected val registrationResult: RegistrationResult   = mock[RegistrationResult]
  protected val registrationFactory: RegistrationFactory = mock[RegistrationFactory]
  protected val eventualRegistrationResult: Future[RegistrationResult] =
    Promise[RegistrationResult].success(registrationResult).future
  protected val eventualDone: Future[Done] = Promise[Done].success(Done).future

  when(registrationFactory.akkaTyped(ArgumentMatchers.any[AkkaConnection], ArgumentMatchers.any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(eventualRegistrationResult)
  when(registrationResult.unregister()).thenReturn(eventualDone)

  override protected def afterAll(): Unit = {
    Await.result(untypedSystem.terminate(), 5.seconds)
    Await.result(system.terminate(), 5.seconds)
  }

  def getSampleHcdWiring(
      componentHandlers: ComponentHandlers[ComponentDomainMsg]
  ): ComponentWiring[ComponentDomainMsg] =
    new ComponentWiring[ComponentDomainMsg] {

      override def handlers(ctx: ActorContext[ComponentMsg],
                            componentInfo: ComponentInfo,
                            pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[ComponentDomainMsg] =
        componentHandlers
    }

  def getSampleAssemblyWiring(
      assemblyHandlers: ComponentHandlers[ComponentDomainMsg]
  ): ComponentWiring[ComponentDomainMsg] =
    new ComponentWiring[ComponentDomainMsg] {
      override def handlers(ctx: ActorContext[ComponentMsg],
                            componentInfo: ComponentInfo,
                            pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[ComponentDomainMsg] =
        assemblyHandlers
    }

}
