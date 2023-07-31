/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.scaladsl

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType, TcpRegistration}
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.commons.*
import csw.location.server.internal.LocationServiceFactory
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.duration.DurationInt

class MultiActorSystemTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val connection      = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "exampleTCPService"), ComponentType.Service))
  private val tcpRegistration = TcpRegistration(connection, 1234)

  private lazy val clusterSettings1 = ClusterSettings().onPort(3558)
  private lazy val clusterSettings2 = ClusterSettings().joinLocal(3558)

  private lazy val system1: ActorSystem[SpawnProtocol.Command] = clusterSettings1.system
  private lazy val system2: ActorSystem[SpawnProtocol.Command] = clusterSettings2.system

  private lazy val locationService1 = LocationServiceFactory.make(clusterSettings1)
  private lazy val locationService2 = LocationServiceFactory.make(clusterSettings2)

  override protected def afterAll(): Unit = {
    system2.terminate()
    system2.whenTerminated.await
  }

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService1.register(tcpRegistration).await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection

    system1.terminate()
    system1.whenTerminated.await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection
  }
}
