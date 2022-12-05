/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit

import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.SecurityDirectives
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.server.internal.{ServerWiring, Settings}
import csw.testkit.internal.{MockedAuthentication, TestKitUtils}

/**
 * LocationTestKit supports starting HTTP Location Server backed by Akka cluster and Distributed Data
 *
 * Example:
 * {{{
 *   private val testKit = LocationTestKit()
 *
 *   // starting Location Server (starts location server on default ports specified in configuration file)
 *   testKit.startLocationServer()
 *
 *   // stopping alarm server
 *   testKit.shutdownLocationServer()
 *
 * }}}
 */
final class LocationTestKit private (testKitSettings: TestKitSettings, enableAuth: Boolean) extends MockedAuthentication {
  final private lazy val locationWiring =
    if (enableAuth) {
      new ServerWiring(enableAuth) {
        override lazy val settings: Settings = new Settings(config) {
          override val clusterPort: Int = testKitSettings.LocationAuthClusterPort
          override val httpPort: Int    = testKitSettings.LocationAuthHttpPort
        }
        override lazy val securityDirectives: SecurityDirectives = _securityDirectives
        override lazy val clusterSettings: ClusterSettings =
          ClusterAwareSettings.joinLocal(testKitSettings.LocationClusterPort)
      }
    }
    else ServerWiring.make(Some(testKitSettings.LocationClusterPort), Some(testKitSettings.LocationHttpPort), enableAuth = false)

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
    locationServer.foreach(TestKitUtils.terminateHttpServerBinding(_, timeout))
    TestKitUtils.shutdown(shutdown(), timeout.duration)
  }

}

object LocationTestKit {

  /**
   * Create a LocationTestKit
   *
   * When the test has completed you should shutdown the location server
   * with [[LocationTestKit#shutdownLocationServer]].
   */
  def apply(
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load()),
      enableAuth: Boolean = false
  ): LocationTestKit =
    new LocationTestKit(testKitSettings, enableAuth)

  /**
   * Create a LocationTestKit
   *
   * When the test has completed you should shutdown the location server
   * with [[LocationTestKit#shutdownLocationServer]].
   */
  def withAuth(config: Config): LocationTestKit = {
    new LocationTestKit(TestKitSettings(config), true)
  }

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
