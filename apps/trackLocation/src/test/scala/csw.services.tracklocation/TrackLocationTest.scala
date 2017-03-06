package csw.services.tracklocation

import java.net.URI

import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import csw.services.location.common.{ActorRuntime, Networks}
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService
import csw.services.tracklocation.common.TestFutureExtension._
import org.scalatest.{FunSuiteLike, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Test the trackLocation app in-line
  */

class TrackLocationTest extends FunSuiteLike with Matchers with LazyLogging {

  private val actorRuntimePort = 2553
  val runtime = new ActorRuntime("track-location-test", Map("akka.remote.netty.tcp.port" -> actorRuntimePort))
  private val system = runtime.actorSystem

  implicit val timeout = Timeout(60.seconds)
  test("Test with command line args") {
    logger.debug("Test1 started")
    val name = "test1"
    val port = 9999
    implicit val dispatcher = system.dispatcher

    Future {
      TrackLocation.main(
        Array(
          "--name",
          name,
          "--command",
          "sleep 10",
          "--port",
          port.toString,
          "--no-exit"
        ))
    }

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
    val locationService = LocationService.make(runtime)

    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:$port")

    val resolvedConnection: Resolved = locationService.resolve(connection).await

    resolvedConnection shouldBe ResolvedTcpLocation(connection, uri)

    logger.debug(s"$name passed")
    logger.debug("Test1 done")
  }

  //  test("Test with config file") {
  //    logger.debug("Test2 started")
  //    val name = "test2"
  //    val port = 8888
  //    val url = getClass.getResource("/test2.conf")
  //    val configFile = Paths.get(url.toURI).toFile.getAbsolutePath
  //
  //    Future {
  //      TrackLocation.main(Array("--name", name, "--no-exit", configFile))
  //    }
  //
  //    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
  //    val locationsReady = Await.result(LocationService.resolve(Set(connection)), timeout.duration)
  //    logger.debug(s"Found $locationsReady")
  //    assert(locationsReady.locations.size == 1)
  //    val loc = locationsReady.locations.head
  //    assert(loc.isResolved)
  //    assert(loc.connection.connectionType == TcpType)
  //    assert(loc.connection.componentId.name == name)
  //    val tcpLoc = loc.asInstanceOf[ResolvedTcpLocation]
  //    assert(tcpLoc.port == port)
  //    logger.debug(s"$name passed")
  //    logger.debug("Test2 done")
  //  }
}
