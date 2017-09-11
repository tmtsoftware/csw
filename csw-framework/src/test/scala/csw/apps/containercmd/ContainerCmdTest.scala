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
import csw.common.framework.models.{ContainerExternalMessage, ContainerMessage, SupervisorExternalMessage}
import csw.services.location.commons.{BlockingUtils, ClusterAwareSettings}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationDouble}

// DEOPSCSW-171: Starting component from command line
class ContainerCmdTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  implicit val actorSystem: typed.ActorSystem[Nothing] = ActorSystem("test").toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(actorSystem)

  var main: ContainerCmd = _

  override def beforeEach(): Unit = {
    main = new ContainerCmd(ClusterAwareSettings.onPort(3552))
  }

  override def afterEach(): Unit = {
    Await.result(main.shutdown, 5.seconds)
  }

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  test("should start container with components") {
    val config    = getClass.getResource("/test_container.conf").getPath
    val args      = Array("--local", config)
    val testProbe = TestProbe[ContainerMode]

    val containerRef = main.start(args).get.asInstanceOf[ActorRef[ContainerMessage]]

    containerRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Idle)

    waitForContainerToMoveIntoRunningMode(containerRef, testProbe, 2.seconds) shouldBe true
  }

  test("should start component in standalone mode") {
    val config    = getClass.getResource("/test_standalone.conf").getPath
    val args      = Array("--standalone", "--local", config)
    val testProbe = TestProbe[SupervisorMode]

    val supervisorRef = main.start(args).get.asInstanceOf[ActorRef[SupervisorExternalMessage]]

    supervisorRef ! GetSupervisorMode(testProbe.ref)
    testProbe.expectMsg(SupervisorMode.Idle)

    waitForSupervisorToMoveIntoRunningMode(supervisorRef, testProbe, 2.seconds) shouldBe true
  }

  def waitForContainerToMoveIntoRunningMode(
      actorRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerMode],
      duration: Duration
  ): Boolean = {

    def getContainerMode: ContainerMode = {
      actorRef ! GetContainerMode(probe.ref)
      probe.expectMsgType[ContainerMode]
    }

    BlockingUtils.poll(getContainerMode == ContainerMode.Running, duration)
  }

  def waitForSupervisorToMoveIntoRunningMode(
      actorRef: ActorRef[SupervisorExternalMessage],
      probe: TestProbe[SupervisorMode],
      duration: Duration
  ): Boolean = {

    def getSupervisorMode: SupervisorMode = {
      actorRef ! GetSupervisorMode(probe.ref)
      probe.expectMsgType[SupervisorMode]
    }

    BlockingUtils.poll(getSupervisorMode == SupervisorMode.Running, duration)
  }
}
