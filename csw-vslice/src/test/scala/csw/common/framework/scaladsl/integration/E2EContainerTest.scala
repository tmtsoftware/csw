package csw.common.framework.scaladsl.integration

import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import csw.common.framework.models.Components
import csw.common.framework.models.ContainerMsg.GetComponents
import csw.common.framework.scaladsl.Component
import org.scalatest.{FunSuite, Matchers}

class E2EContainerTest extends FunSuite with Matchers {
  implicit val system: ActorSystem[Nothing]     = ActorSystem(Actor.empty, "system", Props.empty)
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)

  test("should start multiple components withing a single container") {
    val containerRef = Component.createContainer(ConfigFactory.load("container.conf"))
    Thread.sleep(200)

    val testProbe = TestProbe[Components]
    containerRef ! GetComponents(testProbe.ref)
    val components = testProbe.expectMsgType[Components]
    components.components.size shouldBe 3
  }

}
