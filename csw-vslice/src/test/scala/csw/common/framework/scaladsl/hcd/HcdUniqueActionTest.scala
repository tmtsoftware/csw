package csw.common.framework.scaladsl.hcd

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.AxisStatistics
import csw.common.framework.models.HcdResponseMode
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.HcdDomainMsg
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class HcdUniqueActionTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("hcd component should be able to handle Domain specific messages") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

    Await.result(
      system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref),
                                    "sampleHcd"),
      5.seconds
    )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.hcdRef ! Run

    val running        = supervisorProbe.expectMsgType[Running]
    val axisStatistics = AxisStatistics(1)
    running.hcdRef ! axisStatistics

    Thread.sleep(1000)
    verify(sampleHcdHandler).onDomainMsg(AxisStatistics(1))
  }
}
