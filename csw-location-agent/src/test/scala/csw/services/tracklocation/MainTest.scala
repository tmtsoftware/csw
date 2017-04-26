package csw.services.tracklocation

import java.io.File
import java.net.URI
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.common.TestFutureExtension.RichFuture
import csw.services.tracklocation.models.Command
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuiteLike, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Test the csw-location-agent app in-line
 */
class MainTest extends FunSuiteLike with Matchers with LazyLogging with BeforeAndAfterEach with BeforeAndAfterAll {

  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))
  import scala.concurrent.ExecutionContext.Implicits.global

  override protected def afterAll(): Unit =
    locationService.shutdown().await

  test("Test with command line args") {
    val trackLocationApp = new Main(ClusterAwareSettings.joinLocal(3552))
    val name             = "test1"
    val port             = 9999

    val completionF = Future {
      trackLocationApp.start(
        Array(
          "--name",
          name,
          "--command",
          "sleep 5",
          "--port",
          port.toString,
          "--no-exit"
        )
      )
    }

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))

    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get
    val tcpLocation      = TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))
    resolvedLocation shouldBe tcpLocation

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(6000)

    locationService.list.await shouldBe List.empty
    completionF.await
  }

  test("Test with config file") {
    val trackLocationApp = new Main(ClusterAwareSettings.joinLocal(3552))
    val name             = "test2"
    val url              = getClass.getResource("/test2.conf")
    val configFile       = Paths.get(url.toURI).toFile.getAbsolutePath

    val config = ConfigFactory.parseFile(new File(configFile))
    val port   = config.getString("test2.port")

    val completionF = Future {
      trackLocationApp.start(
        Array("--name", name, "--no-exit", configFile)
      )
    }

    val connection       = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get
    val tcpLocation      = TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))
    resolvedLocation shouldBe tcpLocation

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propagate test's locationService
    Thread.sleep(6000)
    locationService.list.await shouldBe List.empty
    completionF.await
  }
}
