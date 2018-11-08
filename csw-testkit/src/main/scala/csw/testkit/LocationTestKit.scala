package csw.testkit

import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.location.server.internal.ServerWiring
import csw.testkit.internal.TestKitUtils

final class LocationTestKit private (val testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())) {

  private lazy val locationWiring = ServerWiring.make(testKitSettings.LocationClusterPort)
  import locationWiring.actorRuntime._

  implicit lazy val timeout: Timeout = testKitSettings.DefaultTimeout

  private var locationServer: Option[Http.ServerBinding] = None

  /**
   * Start HTTP location server on default port 7654
   *
   * Location server is required to be running on a machine before starting components. (HCD's, Assemblies etc.)
   */
  def startLocationServer(): Unit = locationServer = Some(TestKitUtils.await(locationWiring.locationHttpService.start(), timeout))

  /**
   * Shutdown HTTP location server
   *
   * When the test has completed, make sure you shutdown location server.
   */
  def shutdownLocationServer(): Unit = {
    locationServer.foreach(binding ⇒ TestKitUtils.await(binding.terminate(timeout.duration), timeout))
    TestKitUtils.coordShutdown(shutdown, timeout.duration)
  }

}

object LocationTestKit {

  /**
   * Create a LocationTestKit
   *
   * When the test has completed you should shutdown the location server
   * with [[LocationTestKit#shutdownLocationServer]].
   */
  def apply(testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())): LocationTestKit =
    new LocationTestKit(testKitSettings)

  /**
   * Java API for creating LocationTestKit
   *
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def create(): LocationTestKit = apply()

  /**
   * Java API for creating LocationTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def create(testKitSettings: TestKitSettings): LocationTestKit = apply(testKitSettings)

}
