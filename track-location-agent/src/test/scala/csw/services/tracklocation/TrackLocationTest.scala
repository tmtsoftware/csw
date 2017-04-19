package csw.services.tracklocation

import java.io.File
import java.net.URI
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.common.TestFutureExtension.RichFuture
import csw.services.tracklocation.models.Command
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuiteLike, Matchers}

import scala.concurrent.duration._

/**
 * Test the trackLocation app in-line
 */
class TrackLocationTest
    extends FunSuiteLike
    with Matchers
    with LazyLogging
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val cswCluster      = CswCluster.withSettings(ClusterSettings().onPort(3552))
  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  override protected def afterAll(): Unit =
    locationService.shutdown().await

  test("Test with command line args") {
    val trackLocationApp = new TrackLocationApp(CswCluster.withSettings(ClusterSettings().joinLocal(3552)))
    val name             = "test1"
    val port             = 9999

    val completionF = trackLocationApp.start(
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

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))

    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get
    val tcpLocation      = new TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))
    resolvedLocation shouldBe tcpLocation

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(6000)

    locationService.list.await shouldBe List.empty
    completionF.await
  }

  test("Test with config file") {
    val trackLocationApp = new TrackLocationApp(CswCluster.withSettings(ClusterSettings().joinLocal(3552)))
    val name             = "test2"
    val url              = getClass.getResource("/test2.conf")
    val configFile       = Paths.get(url.toURI).toFile.getAbsolutePath

    val config = ConfigFactory.parseFile(new File(configFile))
    val port   = config.getString("test2.port")

    val completionF = trackLocationApp.start(
      Array("--name", name, "--no-exit", configFile)
    )

    val connection       = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get
    val tcpLocation      = TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))
    resolvedLocation shouldBe tcpLocation

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propagate test's locationService
    Thread.sleep(6000)
    locationService.list.await shouldBe List.empty
    completionF.await
  }

  test("should not contain leading or trailing spaces in service names") {

    val services = List(" test1 ")
    val port     = 9999

    val illegalArgumentException = intercept[IllegalArgumentException] {
      val trackLocation = new TrackLocation(services, Command("sleep 5", port, 0, true), cswCluster, locationService)
      trackLocation.run()
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has leading and trailing whitespaces"
  }

  test("should not contain '-' in service names") {
    val services = List("test-1")
    val port     = 9999

    val illegalArgumentException = intercept[IllegalArgumentException] {
      val trackLocation = new TrackLocation(services, Command("sleep 5", port, 0, true), cswCluster, locationService)
      trackLocation.run()
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has '-'"
  }
}
