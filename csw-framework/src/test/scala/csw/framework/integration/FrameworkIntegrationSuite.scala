/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.integration
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.FrameworkTestWiring
import csw.location.server.internal.ServerWiring
import org.scalatest.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

trait FrameworkIntegrationSuite
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with MockitoSugar {

  private val locationWiring = new ServerWiring(enableAuth = false)
  val testWiring             = new FrameworkTestWiring()

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = {
    testWiring.shutdown()
    locationWiring.actorRuntime.shutdown().await
  }

}
