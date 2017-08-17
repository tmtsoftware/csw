package csw.common.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.components.ComponentDomainMsg
import csw.common.framework.models.ComponentInfo.{AssemblyInfo, HcdInfo}
import csw.common.framework.models.LocationServiceUsages.DoNotRegister
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.{ComponentInfo, ComponentMsg}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class FrameworkComponentTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings    = TestKitSettings(system)
  implicit val timeout: Timeout             = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }

  val assemblyInfo = AssemblyInfo("trombone",
                                  "wfos",
                                  "csw.common.components.SampleComponentWiring",
                                  DoNotRegister,
                                  Set(AkkaType),
                                  Set.empty)

  val hcdInfo =
    HcdInfo("SampleHcd", "wfos", "csw.common.components.SampleComponentWiring", DoNotRegister, Set(AkkaType))

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
