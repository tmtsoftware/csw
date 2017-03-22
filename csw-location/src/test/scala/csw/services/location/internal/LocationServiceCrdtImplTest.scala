package csw.services.location.internal

import java.net.URI

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models._
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.scaladsl.ActorRuntime
import org.scalatest.{FunSuite, Matchers}

class LocationServiceCrdtImplTest extends FunSuite with Matchers {

  test("register-unregister") {
    val actorRuntime = new ActorRuntime("test")
    val crdtImpl = new LocationServiceCrdtImpl(actorRuntime)

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)
    val uri = new URI(s"tcp://${actorRuntime.hostname}:$Port")
    val location = ResolvedTcpLocation(connection, uri)

    val result = crdtImpl.register(location).await

    crdtImpl.resolve(connection).await.get shouldBe location
    crdtImpl.list.await shouldBe List(location)

    result.unregister().await

    crdtImpl.resolve(connection).await shouldBe None
    crdtImpl.list.await shouldBe List.empty
    actorRuntime.terminate().await
  }

  test("tracking") {
    val actorRuntime = new ActorRuntime("test")
    import actorRuntime._

    val crdtImpl = new LocationServiceCrdtImpl(actorRuntime)

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)
    val uri = new URI(s"tcp://${actorRuntime.hostname}:$Port")
    val location = ResolvedTcpLocation(connection, uri)

    val (switch, probe) = crdtImpl.track(connection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    val result = crdtImpl.register(location).await
    probe.request(1)
    probe.expectNext(LocationUpdated(location))

    result.unregister().await
    probe.request(1)
    probe.expectNext(LocationRemoved(connection))

    switch.shutdown()
    probe.request(1)
    probe.expectComplete()

    actorRuntime.terminate().await
  }
}
