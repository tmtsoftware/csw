package csw.services.tracklocation

import java.net.URI
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.utils.CswTestSuite
import csw.services.tracklocation.common.TestFutureExtension.RichFuture

import scala.concurrent.duration._

/**
 * Test the csw-location-agent app in-line
 */
class MainTest extends CswTestSuite {

  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  override protected def afterAllTests(): Unit = locationService.shutdown().await

  test("Test with command line args") {
    val name = "test1"
    val port = 9999
    val args = Array("--name", name, "--command", "sleep 1000", "--port", port.toString, "--no-exit")
    testWith(args, name, port)
  }

  test("Test with config file") {
    val name       = "test2"
    val url        = getClass.getResource("/test2.conf")
    val configFile = Paths.get(url.toURI).toFile
    val config     = ConfigFactory.parseFile(configFile)
    val port       = config.getInt("test2.port")

    val args = Array("--name", name, "--no-exit", configFile.getAbsolutePath)
    testWith(args, name, port)
  }

  private def testWith(args: Array[String], name: String, port: Int) = {
    val trackLocationApp = new Main(ClusterAwareSettings.joinLocal(3552))
    val process          = trackLocationApp.start(args).get

    val connection       = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedLocation = locationService.resolve(connection, 5.seconds).await.get

    resolvedLocation shouldBe TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))

    process.destroy()
    Thread.sleep(1000)
    locationService.list.await shouldBe List.empty
  }
}
