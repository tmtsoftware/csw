package csw.services.location

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.typed.Behavior
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.messages.RunningMessage.DomainMessage
import csw.messages.TMTSerializable
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location._
import csw.services.location.commons.RegistrationFactory
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.location.scaladsl.LocationService
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration._

class LocationServiceTestMultiJvmNode1 extends LocationServiceTest(0)
class LocationServiceTestMultiJvmNode2 extends LocationServiceTest(0)

class LocationServiceTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with BeforeAndAfterEach {

  import config._
  import cswCluster.mat

  val assemblyActorSystem = ActorSystem("assembly-actor-system")

  override protected def afterEach(): Unit =
    Await.result(locationService.unregisterAll(), 10.seconds)

  override def afterAll(): Unit = {
    super.afterAll()
    assemblyActorSystem.terminate()
  }

  test("ensure that a component registered by one node is resolved and listed on all the nodes") {
    val tcpPort         = 446
    val tcpConnection   = TcpConnection(ComponentId("reddis", ComponentType.Service))
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

  test(
    "ensure that component is able to resolve and send message to remote actor created through separate actor system than LocationService actor system"
  ) {

    val tcpConnection  = TcpConnection(ComponentId("redis1", ComponentType.Service))
    val akkaConnection = AkkaConnection(ComponentId("Assembly1", ComponentType.Assembly))

    runOn(seed) {
      val tcpPort         = 470
      val tcpRegistration = RegistrationFactory.tcp(tcpConnection, tcpPort)

      locationService.register(tcpRegistration).await
      enterBarrier("Registration")

      val resolvedLocation = Await.result(locationService.resolve(akkaConnection, 5.seconds), 5.seconds).get

      val assemblyActorRef = resolvedLocation.componentRef()

      assemblyActorRef ! UnregisterConnection(akkaConnection)
      Thread.sleep(2000)
      enterBarrier("Unregistration")

      val locations                    = locationService.list.await
      val connections: Set[Connection] = locations.map(_.connection).toSet

      locations.size shouldBe 1
      connections shouldBe Set(tcpConnection)
    }

    runOn(member) {
      val actorRef = assemblyActorSystem.actorOf(AssemblyActor.props(locationService), "assembly-actor")
      import akka.typed.scaladsl.adapter._
      locationService.register(RegistrationFactory.akka(akkaConnection, actorRef)).await

      enterBarrier("Registration")

      Thread.sleep(2000)

      enterBarrier("Unregistration")
    }

    enterBarrier("after-4")
  }

}

case class UnregisterConnection(akkaConnection: AkkaConnection) extends DomainMessage with TMTSerializable

class AssemblyActor(locationService: LocationService) extends Actor {
  override def receive: Receive = {
    case UnregisterConnection(akkaConnection) => {
      Await.result(locationService.unregister(akkaConnection), 10.seconds)
    }
  }
}

object AssemblyActor {
  def props(locationService: LocationService): Props = Props(new AssemblyActor(locationService))
}
