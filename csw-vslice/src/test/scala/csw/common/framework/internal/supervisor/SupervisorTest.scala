package csw.common.framework.internal.supervisor

import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class SupervisorTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  ignore("Supervisor") {

    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]

    val supervisor =
      Await.result(
        system.systemActorOf(Supervisor.behavior(hcdInfo, getSampleHcdFactory(sampleHcdHandler)), "sampleSupervisor"),
        5.seconds
      )

  }

}
