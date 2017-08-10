package csw.common.framework.scaladsl

import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.Shutdown
import csw.common.framework.scaladsl.testdata.FrameworkTestData
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class CompWiringTest extends FrameworkComponentTestSuite with Matchers with MockitoSugar {

  test(
    "supervisor should be able to get component behavior factory after it's instance is created using reflection from the component info name"
  ) {

    val supervisor =
      Await.result(system.systemActorOf(SupervisorBehaviorFactory.make(FrameworkTestData.hcdInfo), "sampleHcd"),
                   5.seconds)

    Thread.sleep(1000)
    supervisor ! Lifecycle(Shutdown)
  }
}
