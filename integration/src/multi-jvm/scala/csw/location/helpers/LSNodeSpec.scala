/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.helpers

import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import org.apache.pekko.testkit.ImplicitSender
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterSettings
import csw.location.server.internal.LocationServiceFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T, mode: String = "cluster")
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = system.toTyped.asInstanceOf[ActorSystem[SpawnProtocol.Command]]
  protected lazy val clusterSettings: ClusterSettings = ClusterSettings.make(typedSystem)
  lazy protected val locationService: LocationService = mode match {
    case "http"    => HttpLocationServiceFactory.makeLocalClient(typedSystem)
    case "cluster" => LocationServiceFactory.make(clusterSettings)
  }
  lazy val testPrefix = s"${myself.name}"
  lazy val testPrefixWithSuite = s"${this.suiteName}:${testPrefix}"

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  test(s"${testPrefixWithSuite} ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }

}
