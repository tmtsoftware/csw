package csw.services.integtration.tests

import java.io.{BufferedWriter, FileWriter}
import java.net.URI

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, Location, ResolvedTcpLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import csw.services.tracklocation.TrackLocationApp
import org.scalatest._

import scala.concurrent.Future

class TrackLocationAppIntegrationTest
  extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val actorRuntime = new ActorRuntime("track-location-test", 2557)

  import actorRuntime._

  private val locationService = LocationServiceFactory.make(actorRuntime)
  var trackLocationApp: TrackLocationApp = _

  override protected def afterEach(): Unit = {
    //TODO: write and invoke test utility method for unregistering all services
    trackLocationApp.shutdown().await
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
    locationService.shutdown().await
  }

  test("launch the trackLocationApp") {
    val name = "test1"
    val port = 9999

    trackLocationApp = new TrackLocationApp()

    Future {
      trackLocationApp.start(
        Array(
          "--name", name,
          "--command",
          "sleep 5",
          "--port", port.toString,
          "--no-exit"
        )
      )
    }

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
    val uri = new URI(s"tcp://${actorRuntime.ipaddr.getHostAddress}:$port")
    val resolvedConnection = locationService.resolve(connection).await
    resolvedConnection shouldBe ResolvedTcpLocation(connection, uri)

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(10000)

    val locations: Seq[Location] = locationService.list.await
    locations.contains(resolvedConnection) shouldBe false
  }

  test("Test with config file") {
    val name = "test2"
    val port = 8888

    val tempFile = java.io.File.createTempFile("trackLocationApp-test2", ".conf")
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(s"""test2 {
      port = 8888
      command = sleep 5
    }""")
    bw.close()

    val configFile: String = tempFile.getPath
    println(configFile)

    trackLocationApp = new TrackLocationApp()

    Future {
      trackLocationApp.start(
        Array(
          "--name",
          name,
          "--no-exit",
          configFile)
      )
    }

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
    val resolvedConnection = locationService.resolve(connection).await
    val uri = new URI(s"tcp://${actorRuntime.ipaddr.getHostAddress}:$port")
    resolvedConnection shouldBe ResolvedTcpLocation(connection, uri)

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(15000)

    val locations: Seq[Location] = locationService.list.await
    locations.contains(resolvedConnection) shouldBe false

    tempFile.delete()
  }
}