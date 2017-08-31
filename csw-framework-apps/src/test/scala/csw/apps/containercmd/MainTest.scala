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
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-171: Starting component from command line
class MainTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  implicit val actorSystem: typed.ActorSystem[Nothing] = ActorSystem("test").toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(actorSystem)

  var main: Main = _

  override def beforeEach(): Unit = {
    main = new Main(ClusterAwareSettings.onPort(3552))
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

    val maybeContainerRef = main.start(args).get.asInstanceOf[ActorRef[ContainerMessage]]

    maybeContainerRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Idle)

    Thread.sleep(500)

    maybeContainerRef ! GetContainerMode(testProbe.ref)
    testProbe.expectMsg(ContainerMode.Running)
  }

  test("should start component in standalone mode") {
    val config    = getClass.getResource("/test_standalone.conf").getPath
    val args      = Array("--standalone", "--local", config)
    val testProbe = TestProbe[SupervisorMode]

    val maybeSupervisorRef = main.start(args).get.asInstanceOf[ActorRef[SupervisorExternalMessage]]

    maybeSupervisorRef ! GetSupervisorMode(testProbe.ref)
    testProbe.expectMsg(SupervisorMode.Idle)

    Thread.sleep(500)

    maybeSupervisorRef ! GetSupervisorMode(testProbe.ref)
    testProbe.expectMsg(SupervisorMode.Running)
  }

}
