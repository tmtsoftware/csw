/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.integtration.tests

import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.internal.ServerWiring
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.util.control.NonFatal

class LocationServiceMultipleNICTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with ScalaFutures with Eventually {

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, org.scalatest.time.Seconds), Span(100, org.scalatest.time.Millis))

  private val locationWiring = ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"), enableAuth = false)
  locationWiring.actorRuntime.startLogging("Test", locationWiring.clusterSettings.hostname)
  locationWiring.locationHttpService.start().futureValue

  val runtime = locationWiring.actorRuntime
  import runtime._

  private val locationService = HttpLocationServiceFactory.makeLocalClient

  override def afterAll(): Unit = Await.result(locationWiring.actorRuntime.shutdown(), 5.seconds)

  test("should list and resolve component having multiple-nic's") {
    val componentId = ComponentId(Prefix(Subsystem.NFIRAOS, "assembly"), ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    try {
      eventually(locationService.list.await.size shouldBe 1)
      locationService.find(connection).await.get shouldBe a[AkkaLocation]
    }
    catch {
      case NonFatal(e) => // this is required so that docker understands when test fails
        e.printStackTrace()
        sys.exit(1)
    }
  }
}
