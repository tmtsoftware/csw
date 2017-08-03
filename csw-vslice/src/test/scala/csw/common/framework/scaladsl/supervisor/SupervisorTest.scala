package csw.common.framework.scaladsl.supervisor

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.Component.{DoNotRegister, HcdInfo}
import csw.common.framework.models.ComponentMsg
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.scaladsl.hcd.{HcdBehaviorFactory, HcdHandlers}
import csw.param.StateVariable.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.FiniteDuration

class SupervisorTest extends FunSuite with Matchers with MockitoSugar {

  def getSampleHcdFactory(hcdHandlers: HcdHandlers[HcdDomainMsg]): HcdBehaviorFactory[HcdDomainMsg] =
    new HcdBehaviorFactory[HcdDomainMsg] {
      override def make(ctx: ActorContext[ComponentMsg],
                        hcdInfo: HcdInfo,
                        pubSubRef: ActorRef[PublisherMsg[CurrentState]]): HcdHandlers[HcdDomainMsg] = hcdHandlers
    }

  test("") {
    val hcdInfo = HcdInfo("SampleHcd",
                          "wfos",
                          "csw.common.components.hcd.SampleHcd",
                          DoNotRegister,
                          Set(AkkaType),
                          FiniteDuration(5, "seconds"))

    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]

    val supervisorBeh = Supervisor.behavior(hcdInfo, getSampleHcdFactory(sampleHcdHandler))

  }
}
