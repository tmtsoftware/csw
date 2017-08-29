package csw.apps.containercmd

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
import csw.common.framework.models.{ContainerMessage, SupervisorExternalMessage}
import csw.services.location.commons.ClusterAwareSettings
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class MainTest extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val actorSystem: typed.ActorSystem[Nothing] = ActorSystem("test").toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(actorSystem)

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  class TestData[T](args: Array[String]) {
    val main = new Main(ClusterAwareSettings.onPort(3552))
    val maybeRef: ActorRef[T] =
      main
        .start(args)
        .get
        .asInstanceOf[ActorRef[T]]
  }

  test("should start container with components") {
    val config    = getClass.getResource("/test_container.conf").getPath
    val args      = Array("--local", config)
    val testProbe = TestProbe[ContainerMode]
    val testData  = new TestData[ContainerMessage](args)
    import testData._

    maybeRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Idle)

    Thread.sleep(500)

    maybeRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Running)

    Await.result(main.shutdown, 5.seconds)
  }

  test("should start component in standalone mode") {
    val config    = getClass.getResource("/test_standalone.conf").getPath
    val args      = Array("--standalone", "--local", config)
    val testProbe = TestProbe[SupervisorMode]
    val testData  = new TestData[SupervisorExternalMessage](args)
    import testData._

    maybeRef ! GetSupervisorMode(testProbe.ref)
    testProbe.expectMsg(SupervisorMode.Idle)

    Thread.sleep(500)

    maybeRef ! GetSupervisorMode(testProbe.ref)
    testProbe.expectMsg(SupervisorMode.Running)

    Await.result(main.shutdown, 5.seconds)
  }

}
