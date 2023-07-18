/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.internal

import org.apache.pekko.actor.typed.SpawnProtocol
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.*
import csw.location.client.ActorSystemFactory
import csw.location.server.commons.CswCluster
import csw.prefix.models.{Prefix, Subsystem}
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class LocationServiceImplTest extends AnyFunSuite with Matchers with MockitoSugar {
  private val system         = ActorSystemFactory.remote(SpawnProtocol(), "test-system")
  private val mockCswCluster = mock[CswCluster]
  private val httpConnection = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))
  private val port           = 5003

  test("should select public hostname when network type is public | CSW-97") {
    val registration = HttpRegistration(connection = httpConnection, port = port, path = "", NetworkType.Outside)

    when(mockCswCluster.publicHostname) thenReturn ("some-public-ip")

    val locationService = new LocationServiceImpl(mockCswCluster)
    locationService.getLocation(registration).uri.getHost shouldBe "some-public-ip"
  }

  test("should select private hostname when no network type provided | CSW-97") {
    val registration = HttpRegistration(connection = httpConnection, port = port, path = "")

    when(mockCswCluster.hostname) thenReturn ("some-private-ip")

    val locationService = new LocationServiceImpl(mockCswCluster)
    locationService.getLocation(registration).uri.getHost shouldBe "some-private-ip"
  }

  test("should not use public or private cluster hostname for Pekko Registration | CSW-97") {
    val componentId: ComponentId         = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
    val pekkoConnection: PekkoConnection = PekkoConnection(componentId)
    val actorRef                         = system.systemActorOf(Behaviors.empty, "test-actor")
    val pekkoRegistration: PekkoRegistration =
      PekkoRegistrationFactory.make(pekkoConnection, actorRef)

    when(mockCswCluster.hostname) thenReturn ("some-private-ip")
    when(mockCswCluster.publicHostname) thenReturn ("some-public-ip")

    val locationService = new LocationServiceImpl(mockCswCluster)
    locationService.getLocation(pekkoRegistration).uri shouldBe actorRef.toURI
    locationService.getLocation(pekkoRegistration).uri.toString should not include "some-private-ip"
    locationService.getLocation(pekkoRegistration).uri.toString should not include "some-public-ip"
  }
}
