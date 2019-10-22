package csw.location

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Keep, Sink}
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models._
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.params.core.models.Prefix

class TrackLocationTestMultiJvmNode1 extends TrackLocationTest(0, "cluster")
class TrackLocationTestMultiJvmNode2 extends TrackLocationTest(0, "cluster")
class TrackLocationTestMultiJvmNode3 extends TrackLocationTest(0, "cluster")

class TrackLocationTest(ignore: Int, mode: String) extends LSNodeSpec(config = new TwoMembersAndSeed, mode) {

  import config._
  import cswCluster.mat

  // DEOPSCSW-26: Track a connection
  test("two components should able to track same connection and single component should able to track two components") {
    //create akka connection
    val akkaConnection = AkkaConnection(ComponentId("tromboneHcd", ComponentType.HCD))

    //create http connection
    val httpConnection = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))

    //create tcp connection
    val tcpConnection = TcpConnection(models.ComponentId("redis1", ComponentType.Service))

    val prefix = Prefix("nfiraos.ncc.trombone")

    runOn(seed) {
      val actorRef = cswCluster.typedSystem.spawn(Behaviors.empty, "trombone-hcd")
      locationService.register(AkkaRegistrationFactory.make(akkaConnection, prefix, actorRef.toURI)).await
      enterBarrier("Registration")

      locationService.unregister(akkaConnection).await
      enterBarrier("Akka-unregister")
      enterBarrier("Http-unregister")
      enterBarrier("Tcp-unregister")
    }

    runOn(member1) {
      val port   = 5656
      val prefix = "/trombone/hcd"

      val httpRegistration       = HttpRegistration(httpConnection, port, prefix)
      val httpRegistrationResult = locationService.register(httpRegistration).await
      val akkaProbe              = TestProbe[TrackingEvent]("test-probe1")
      val tcpProbe               = TestProbe[TrackingEvent]("test-probe2")

      locationService.track(akkaConnection).toMat(Sink.foreach(akkaProbe.ref.tell(_)))(Keep.left).run()
      locationService.track(tcpConnection).toMat(Sink.foreach(tcpProbe.ref.tell(_)))(Keep.left).run()

      val akkaEvent             = akkaProbe.expectMessageType[LocationUpdated]
      val trackedAkkaConnection = akkaEvent.asInstanceOf[LocationUpdated].connection
      trackedAkkaConnection shouldBe akkaConnection

      val tcpEvent: LocationUpdated = tcpProbe.expectMessageType[LocationUpdated]
      tcpEvent.connection shouldBe tcpConnection

      enterBarrier("Registration")
      enterBarrier("Akka-unregister")

      val akkaRemovedEvent: LocationRemoved = akkaProbe.expectMessageType[LocationRemoved]
      akkaRemovedEvent.connection shouldBe akkaConnection

      httpRegistrationResult.unregister().await
      enterBarrier("Http-unregister")
      enterBarrier("Tcp-unregister")

      val tcpRemovedEvent: LocationRemoved = tcpProbe.expectMessageType[LocationRemoved]
      tcpRemovedEvent.connection shouldBe tcpConnection
    }

    runOn(member2) {
      val Port                  = 5657
      val tcpRegistration       = TcpRegistration(tcpConnection, Port)
      val tcpRegistrationResult = locationService.register(tcpRegistration).await

      val httpProbe = TestProbe[TrackingEvent]("test-probe")

      locationService.track(httpConnection).toMat(Sink.foreach(httpProbe.ref.tell(_)))(Keep.left).run()

      val httpEvent: TrackingEvent = httpProbe.expectMessageType[LocationUpdated]
      httpEvent.connection shouldBe httpConnection

      enterBarrier("Registration")
      enterBarrier("Akka-unregister")
      enterBarrier("Http-unregister")

      val httpRemovedEvent: TrackingEvent = httpProbe.expectMessageType[LocationRemoved]
      httpRemovedEvent.connection shouldBe httpConnection

      tcpRegistrationResult.unregister().await
      enterBarrier("Tcp-unregister")
    }

    enterBarrier("after-2")
  }
}
