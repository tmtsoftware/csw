/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client

import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.mocks.MockedAuthentication
import csw.location.server.internal.ServerWiring
import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

trait ConfigClientBaseSuite
    extends MockedAuthentication
    with AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {
  private val locationWiring = new ServerWiring(enableAuth = false)

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = locationWiring.actorRuntime.shutdown().await

}
