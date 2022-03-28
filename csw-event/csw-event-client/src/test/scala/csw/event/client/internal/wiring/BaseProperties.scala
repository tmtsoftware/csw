/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.wiring

import java.net.URI

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Attributes
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.location.api.models
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

trait BaseProperties {
  val eventPattern: String
  val publisher: EventPublisher
  val subscriber: EventSubscriber
  val eventService: EventService
  val jEventService: IEventService
  val jPublisher: IEventPublisher
  val jSubscriber: IEventSubscriber
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
