package csw.services.location.impl

import java.net.URI
import javax.jmdns.ServiceInfo

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import csw.services.location.internal.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.internal.{Constants, Networks}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.ActorRuntime
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import csw.services.location.common.TestFutureExtension.RichFuture

class RichServiceInfoTest
  extends FunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll{

  val actorRuntimePort = 2554
  private val actorRuntime = new ActorRuntime("test", Map("akka.remote.netty.tcp.port" -> actorRuntimePort))

  import actorRuntime.actorSystem

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }

  test("test HTTP ServiceInfo to HTTP Service Locations when location information is present") {
    val componentId = ComponentId("configService", ComponentType.Service)
    val connection = HttpConnection(componentId)
    val path = "test-path"
    val values: Map[String, String] = Map(
      Constants.PathKey -> path
    )
    val protocol = connection.connectionType.name
    val url = s"${protocol}://${Networks.getIpv4Address()}:$actorRuntimePort/$path"
    val resolvedHttpLocation = ResolvedHttpLocation(connection, new URI(url), path)

    val httpServiceInfo = mock[ServiceInfo]
    when(httpServiceInfo.getName).thenReturn(connection.name)
    when(httpServiceInfo.getType).thenReturn(protocol)
    when(httpServiceInfo.getDomain()).thenReturn(connection.componentId.componentType.name)
    when(httpServiceInfo.getApplication()).thenReturn(connection.name)
    when(httpServiceInfo.getURLs(protocol)).thenReturn(Array(url))
    when(httpServiceInfo.getPropertyString(Constants.PathKey)).thenReturn(values(Constants.PathKey))

    val locations = httpServiceInfo.locations

    locations should contain(resolvedHttpLocation)
  }

  test("test TCP ServiceInfo to TCP Service Locations when location information is present") {
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)
    val protocol = connection.connectionType.name
    val url = s"${protocol}://${Networks.getIpv4Address()}:$actorRuntimePort"
    val resolvedTcpLocation = ResolvedTcpLocation(connection, new URI(url))

    val tcpServiceInfo = mock[ServiceInfo]
    when(tcpServiceInfo.getName).thenReturn(connection.name)
    when(tcpServiceInfo.getType).thenReturn(protocol)
    when(tcpServiceInfo.getDomain()).thenReturn(connection.componentId.componentType.name)
    when(tcpServiceInfo.getApplication()).thenReturn(connection.name)
    when(tcpServiceInfo.getURLs(protocol)).thenReturn(Array(url))

    val locations = tcpServiceInfo.locations

    locations should contain(resolvedTcpLocation)
  }

  test("test Actor ServiceInfo to Actor Service Locations when location information is present") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"

    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )

    val protocol = connection.connectionType.name
    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
    val uri = new URI(actorPath.toString)
    val resolvedAkkaLocation = ResolvedAkkaLocation(connection, uri, Prefix, Some(actorRef))
    val url = ""

    val akkaServiceInfo = mock[ServiceInfo]
    when(akkaServiceInfo.getName).thenReturn(connection.name)
    when(akkaServiceInfo.getType).thenReturn(protocol)
    when(akkaServiceInfo.getDomain()).thenReturn(connection.componentId.componentType.name)
    when(akkaServiceInfo.getApplication()).thenReturn(connection.name)
    when(akkaServiceInfo.getURLs(protocol)).thenReturn(Array(url))
    when(akkaServiceInfo.getPropertyString(Constants.PrefixKey)).thenReturn(Prefix)
    when(akkaServiceInfo.getPropertyString(Constants.ActorPathKey)).thenReturn(actorPath.toString)

    val locations = akkaServiceInfo.locations
    locations.foreach(println)

    locations should contain(resolvedAkkaLocation)
  }
}
