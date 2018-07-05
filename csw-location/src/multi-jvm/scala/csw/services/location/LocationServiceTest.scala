package csw.services.location

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location._
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.TestRegistrationFactory
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import org.scalatest.BeforeAndAfterEach

import scala.collection.immutable.Set
import scala.concurrent.Await
import scala.concurrent.duration._

class LocationServiceTestMultiJvmNode1 extends LocationServiceTest(0, "cluster")
class LocationServiceTestMultiJvmNode2 extends LocationServiceTest(0, "cluster")

class LocationServiceTest(ignore: Int, mode: String)
    extends LSNodeSpec(config = new OneMemberAndSeed, mode)
    with BeforeAndAfterEach {

  import config._
  import cswCluster.mat

  val RegistrationFactory = new TestRegistrationFactory

  override protected def afterEach(): Unit =
    Await.result(locationService.unregisterAll(), 10.seconds)

  test("ensure that a component registered by one node is resolved and listed on all the nodes") {
    val tcpPort         = 446
    val tcpConnection   = TcpConnection(ComponentId("redis", ComponentType.Service))
    val tcpRegistration = RegistrationFactory.tcp(tcpConnection, tcpPort)

    val httpPort         = 81
    val httpPath         = "/test/hcd"
    val httpConnection   = HttpConnection(ComponentId("tromboneHcd", ComponentType.HCD))
    val httpRegistration = RegistrationFactory.http(httpConnection, httpPort, httpPath)

    runOn(seed) {
      locationService.register(tcpRegistration).await
      enterBarrier("Registration")

      val resolvedHttpLocation = locationService.resolve(httpConnection, 5.seconds).await.get
      resolvedHttpLocation.connection shouldBe httpConnection

      val locations   = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    runOn(member) {
      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      val resolvedTcpLocation = locationService.resolve(tcpConnection, 5.seconds).await.get
      resolvedTcpLocation.connection shouldBe tcpConnection

      val locations   = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    enterBarrier("after-2")
  }

//  This test is doing the same thing what TrackLocationTest is doing
//  but the plan is to run this test on two amazon instance's with Jenkins configuration (multiNodeTest).
  test("ensure that a component registered on one node is tracked on all the nodes") {
    val componentId    = ComponentId("tromboneHcd", ComponentType.HCD)
    val akkaConnection = AkkaConnection(componentId)

    runOn(seed) {
      val actorRef = cswCluster.actorSystem.spawn(Behavior.empty, "trombone-hcd")
      locationService.register(RegistrationFactory.akka(akkaConnection, actorRef)).await
      enterBarrier("Registration")

      locationService.unregister(akkaConnection).await
      enterBarrier("Unregister")

      locationService.register(RegistrationFactory.akka(akkaConnection, actorRef)).await
      enterBarrier("Re-registration")
    }

    runOn(member) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

      probe.request(1)
      probe.requestNext() shouldBe a[LocationUpdated]
      enterBarrier("Registration")

      probe.request(1)
      probe.requestNext() shouldBe a[LocationRemoved]
      enterBarrier("Unregister")

      probe.request(1)
      probe.requestNext() shouldBe a[LocationUpdated]
      enterBarrier("Re-registration")

      switch.shutdown()
      probe.request(1)
      probe.expectComplete()
    }
    enterBarrier("after-3")
  }
}
