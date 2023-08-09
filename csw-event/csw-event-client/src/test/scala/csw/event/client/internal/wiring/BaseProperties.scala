/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.wiring

import java.net.URI

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.stream.Attributes
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.location.api.models
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import cps.compat.FutureAsync.*
import scala.concurrent.{ExecutionContext, Future}

trait BaseProperties {
  val eventPattern: String
  def publisher: EventPublisher
  def subscriber: EventSubscriber
  val eventService: EventService
  val jEventService: IEventService
  def jPublisher: IEventPublisher
  def jSubscriber: IEventSubscriber
  def publishGarbage(channel: String, message: String): Future[Done]
  def start(): Unit
  def shutdown(): Unit

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command]
  implicit lazy val ec: ExecutionContext = actorSystem.executionContext
  val attributes: Attributes             = EventStreamSupervisionStrategy.attributes

  def resolveEventService(locationService: LocationService): Future[URI] =
    async {
      val eventServiceResolver = new EventServiceLocationResolver(locationService)
      await(eventServiceResolver.uri())
    }
}

object BaseProperties {
  def createInfra(serverPort: Int, httpPort: Int): (LocationService, ActorSystem[SpawnProtocol.Command]) = {

    implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "event-server")

    val locationService = HttpLocationServiceFactory.make("localhost", httpPort)
    val tcpRegistration = models.TcpRegistration(EventServiceConnection.value, serverPort)

    locationService.register(tcpRegistration).await
    (locationService, typedSystem)
  }
}
