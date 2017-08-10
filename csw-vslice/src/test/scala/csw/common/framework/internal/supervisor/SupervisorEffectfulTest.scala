package csw.common.framework.internal.supervisor

import akka.typed.{Behavior, Terminated}
import csw.Effect.Stopped
import csw.EffectfulActorContext
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.SupervisorMsg
import csw.common.framework.scaladsl.testdata.FrameworkTestData
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite, SupervisorBehaviorFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

class SupervisorEffectfulTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  class TestData {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]
    val supervisorBehavior: Behavior[SupervisorMsg]       = SupervisorBehaviorFactory.make(FrameworkTestData.hcdInfo)
    val ctx =
      new EffectfulActorContext[SupervisorMsg]("supervisor-test", supervisorBehavior, 100, system)
  }

  test("terminated signal") {
    val testData = new TestData
    import testData._

    ctx.signal(Terminated(ctx.getChildren.get(0).ref)(null))

    ctx.getAllEffects() should contain allOf (
      Stopped(Supervisor.PubSubLifecycleActor),
      Stopped(Supervisor.PubSubComponentActor)
    )
  }
}
