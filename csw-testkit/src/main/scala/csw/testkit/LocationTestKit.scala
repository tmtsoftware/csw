package csw.testkit

import java.util.Optional

import akka.http.scaladsl.Http
import akka.util.Timeout
import csw.location.server.internal.ServerWiring
import csw.testkit.internal.TestKitUtils

import scala.compat.java8.OptionConverters.RichOptionalGeneric

final class LocationTestKit private (clusterPort: Option[Int], settings: Option[TestKitSettings]) {

  private lazy val locationWiring = ServerWiring.make(clusterPort)
  import locationWiring.actorRuntime._

  lazy val testKitSettings: TestKitSettings = settings.getOrElse(TestKitSettings(actorSystem))
  implicit lazy val timeout: Timeout        = testKitSettings.DefaultTimeout

  /**
   * Start HTTP location server on default port 7654
   *
   * Location server is required to be running on a machine before starting components. (HCD's, Assemblies etc.)
   */
  def startLocationServer(): Http.ServerBinding = TestKitUtils.await(locationWiring.locationHttpService.start(), timeout)

  /**
   * Shutdown HTTP location server
   *
   * When the test has completed, make sure you shutdown location server.
   */
  def shutdownLocationServer(): Unit = TestKitUtils.coordShutdown(shutdown, timeout.duration)

}

object LocationTestKit {

  /**
   * Create a LocationTestKit
   *
   * When the test has completed you should shutdown the location server
   * with [[LocationTestKit#shutdownLocationServer]].
   *
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def apply(): LocationTestKit = new LocationTestKit(None, None)

  /**
   * Create a LocationTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def apply(testKitSettings: TestKitSettings): LocationTestKit = new LocationTestKit(None, Some(testKitSettings))

  /**
   * Scala API for creating LocationTestKit
   *
   * @param clusterPort port on which akka cluster to be started (backend of location service)
   * @param testKitSettings custom testKitSettings
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def apply(clusterPort: Option[Int], testKitSettings: Option[TestKitSettings]): LocationTestKit =
    new LocationTestKit(clusterPort, testKitSettings)

  /**
   * Java API for creating LocationTestKit
   *
   * @param clusterPort port on which akka cluster to be started (backend of location service)
   * @param testKitSettings custom testKitSettings
   * @return handle to LocationTestKit which can be used to start and stop location server
   */
  def create(clusterPort: Optional[Int], testKitSettings: Optional[TestKitSettings]): LocationTestKit =
    apply(clusterPort.asScala, testKitSettings.asScala)

}
