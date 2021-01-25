package csw.framework.internal.supervisor

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.SupervisorMessage
import csw.command.client.models.framework.{LifecycleStateChanged, SupervisorLifecycleState}
import csw.location.client.ActorSystemFactory
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration.DurationInt

class SupervisorLifecycleTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike with BeforeAndAfterAll {
  import SupervisorLifecycleState._
  import LifecycleHandler._

  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "testSystem")

  //LoggingSystemFactory.start("logging", "1", "localhost", typedSystem)

  //LoggingSystemFactory.forTestingOnly()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("test create and default state") {
    val svrProbe = testKit.createTestProbe[SupervisorMessage]
    val sl       = testKit.spawn(LifecycleHandler(new LoggerFactory(Prefix("ESW.test")), svrProbe.ref))

    val stateProbe = testKit.createTestProbe[StateResponse]

    // test that initial state is idle
    sl ! GetState(stateProbe.ref)
    stateProbe.expectMessage(StateResponse(Idle))
  }

  test("change state") {
    val svrProbe   = testKit.createTestProbe[SupervisorMessage]
    val sl         = testKit.spawn(LifecycleHandler(new LoggerFactory(Prefix("ESW.test")), svrProbe.ref))
    val stateProbe = testKit.createTestProbe[StateResponse]

    // test that initial state is idle
    sl ! GetState(stateProbe.ref)
    stateProbe.expectMessage(StateResponse(Idle))

    sl ! UpdateState(Running)

    // test that state is now Running
    sl ! GetState(stateProbe.ref)
    stateProbe.expectMessage(StateResponse(Running))
  }

  test("subscribe test") {
    val svrProbe = testKit.createTestProbe[SupervisorMessage]
    val sl       = testKit.spawn(LifecycleHandler(new LoggerFactory(Prefix("ESW.test")), svrProbe.ref))
    val s1       = testKit.createTestProbe[LifecycleStateChanged]

    // s1 subscribes
    sl ! SubscribeState(s1.ref)
    s1.expectMessage(LifecycleStateChanged(svrProbe.ref, Idle))

    sl ! UpdateState(Running)

    s1.expectMessage(LifecycleStateChanged(svrProbe.ref, Running))

    val s2 = testKit.createTestProbe[LifecycleStateChanged]
    // s2 subscribes
    sl ! SubscribeState(s2.ref)
    // s2 gets update from subscribing
    s2.expectMessage(LifecycleStateChanged(svrProbe.ref, Running))

    // Update again
    sl ! UpdateState(RunningOffline)
    // Both should get a message
    s1.expectMessage(LifecycleStateChanged(svrProbe.ref, RunningOffline))
    s2.expectMessage(LifecycleStateChanged(svrProbe.ref, RunningOffline))

  }

  test("unsubscribe test") {
    val svrProbe = testKit.createTestProbe[SupervisorMessage]
    val sl       = testKit.spawn(LifecycleHandler(new LoggerFactory(Prefix("ESW.test")), svrProbe.ref))
    val s1       = testKit.createTestProbe[LifecycleStateChanged]

    // s1 subscribes
    sl ! SubscribeState(s1.ref)
    s1.expectMessage(LifecycleStateChanged(svrProbe.ref, Idle))

    sl ! UpdateState(Running)
    // s1 gets an update
    s1.expectMessage(LifecycleStateChanged(svrProbe.ref, Running))

    // s1 unsubscribes
    sl ! UnsubscribeState(s1.ref)

    // Update again
    sl ! UpdateState(RunningOffline)
    // Poor s1 should not get a message
    s1.expectNoMessage(200.millis)
  }
}
