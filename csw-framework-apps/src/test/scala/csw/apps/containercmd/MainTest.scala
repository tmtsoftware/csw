package csw.apps.containercmd

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.ContainerMessage
import csw.services.location.commons.ClusterAwareSettings
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class MainTest extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val actorSystem: typed.ActorSystem[Nothing] = ActorSystem("test").toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(actorSystem)

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  test("should start container with components") {
    val config    = getClass.getResource("/test_container.conf").getPath
    val args      = Array("--local", config)
    val testProbe = TestProbe[ContainerMode]
    val maybeContainerRef: ActorRef[ContainerMessage] =
      new Main(ClusterAwareSettings.onPort(3552))
        .start(args)
        .get
        .asInstanceOf[ActorRef[ContainerMessage]]

    maybeContainerRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Idle)

    maybeContainerRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Running)
  }
}
