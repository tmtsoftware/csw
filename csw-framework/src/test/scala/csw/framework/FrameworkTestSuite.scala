package csw.framework

import akka.actor
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.components.ComponentDomainMessage
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.ComponentMessage
import csw.messages.PubSub.PublisherMessage
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.LocationService
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class FrameworkTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val untypedSystem: actor.ActorSystem = ActorSystemFactory.remote()
  implicit val system: ActorSystem[Nothing]     = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings        = TestKitSettings(system)
  implicit val timeout: Timeout                 = Timeout(5.seconds)
  def frameworkTestMocks(): FrameworkTestMocks  = new FrameworkTestMocks

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
          pubSubRef: ActorRef[PublisherMessage[CurrentState]],
          locationService: LocationService
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
          pubSubRef: ActorRef[PublisherMessage[CurrentState]],
          locationService: LocationService
      ): ComponentHandlers[ComponentDomainMessage] =
        assemblyHandlers
    }

}
