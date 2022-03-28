/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt

private[csw] trait HTTPLocationService
    extends AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with MockitoSugar {

  protected val locationPort: Int             = 3553
  protected val httpLocationPort: Option[Int] = None
  var locationWiring: Option[ServerWiring]    = None
  protected val enableAuth: Boolean           = false

  def start(clusterPort: Option[Int] = Some(locationPort), httpPort: Option[Int] = httpLocationPort): Unit = {
    locationWiring = Some(ServerWiring.make(clusterPort, httpPort, enableAuth = enableAuth))
    locationWiring.map { wiring =>
      val locationBinding = wiring.locationHttpService.start().await
      wiring.actorRuntime.coordinatedShutdown.addTask(
        CoordinatedShutdown.PhaseServiceUnbind,
        "unbind-services"
      )(() => locationBinding.terminate(30.seconds).map(_ => Done)(wiring.actorRuntime.ec))
      wiring
    }
  }

  override def beforeAll(): Unit = start()
  override def afterAll(): Unit = {
    locationWiring.map(_.actorRuntime.shutdown().await)
  }

}

private[csw] class JHTTPLocationService extends HTTPLocationService

private[csw] class HTTPLocationServiceOnPorts(clusterPort: Int, val httpPort: Int, auth: Boolean = false)
    extends HTTPLocationService {
  override val locationPort: Int             = clusterPort
  override val enableAuth: Boolean           = auth
  override val httpLocationPort: Option[Int] = Some(httpPort)
}
