package csw.services.tracklocation

import java.net.URI
import java.nio.file.Paths

import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import csw.services.location.impl.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import csw.services.tracklocation.common.TestFutureExtension._
import csw.services.tracklocation.models.Command
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuiteLike, Matchers}

import scala.concurrent.Future
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

  private val actorRuntimePort = 2553

  private val actorRuntime = new ActorRuntime("track-location-test", Map("akka.remote.netty.tcp.port" -> actorRuntimePort))
  import actorRuntime._
  private val locationService = LocationServiceFactory.make(actorRuntime)

  implicit val timeout = Timeout(60.seconds)

  override protected def afterEach(): Unit = {
    //TODO: write and invoke test utility method for unregistering all services
    TrackLocationApp.shutdown().await
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
  }

  test("Test with command line args") {
    val name = "test1"
    val port = 9999

    Future {
      TrackLocationApp.main(
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

    locationService.list.await shouldBe List.empty
  }

  test("Test with config file") {
    val name = "test2"
    val port = 8888
    val url = getClass.getResource("/test2.conf")
    val configFile = Paths.get(url.toURI).toFile.getAbsolutePath

    Future {
      TrackLocationApp.main(
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
    Thread.sleep(10000)
    locationService.list.await shouldBe List.empty
  }

  test("should not contain leading or trailing spaces in service names") {

    val services = List(" test1 ")
    val port = 9999

    val illegalArgumentException = intercept[IllegalArgumentException] {
      val trackLocation = new TrackLocation(services, Command("sleep 5", port, 0, true), actorRuntime)
      trackLocation.run().await
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has leading and trailing whitespaces"
  }

  test("should not contain '-' in service names") {
    val services = List("test-1")
    val port = 9999

    val illegalArgumentException = intercept[IllegalArgumentException] {
      val trackLocation = new TrackLocation(services, Command("sleep 5", port, 0, true), actorRuntime)
      trackLocation.run().await
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has '-'"
  }
}
