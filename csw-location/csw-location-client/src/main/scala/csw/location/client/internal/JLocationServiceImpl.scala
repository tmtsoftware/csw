/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.client.internal

import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import org.apache.pekko.Done
import org.apache.pekko.stream.javadsl.Source
import csw.location.api.javadsl.{ILocationService, IRegistrationResult}
import csw.location.api.models.*
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import msocket.api.Subscription

import scala.jdk.DurationConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

private[location] class JLocationServiceImpl(locationService: LocationService)(implicit ec: ExecutionContext)
    extends ILocationService {

  override def register(registration: Registration): CompletableFuture[IRegistrationResult] =
    locationService.register(registration).map(registrationResult).asJava.toCompletableFuture

  override def unregister(connection: Connection): CompletableFuture[Done] =
    locationService.unregister(connection).asJava.toCompletableFuture

  override def unregisterAll(): CompletableFuture[Done] =
    locationService.unregisterAll().asJava.toCompletableFuture

  override def find[L <: Location](connection: TypedConnection[L]): CompletableFuture[Optional[L]] =
    locationService.find(connection).map(_.toJava).asJava.toCompletableFuture

  override def resolve[L <: Location](connection: TypedConnection[L], within: Duration): CompletableFuture[Optional[L]] =
    locationService.resolve(connection, within.toScala).map(_.toJava).asJava.toCompletableFuture

  override def list: CompletableFuture[util.List[Location]] =
    locationService.list.map(_.asJava).asJava.toCompletableFuture

  override def list(componentType: ComponentType): CompletableFuture[util.List[Location]] =
    locationService.list(componentType).map(_.asJava).asJava.toCompletableFuture

  override def list(hostname: String): CompletableFuture[util.List[Location]] =
    locationService.list(hostname).map(_.asJava).asJava.toCompletableFuture

  override def list(connectionType: ConnectionType): CompletableFuture[util.List[Location]] =
    locationService.list(connectionType).map(_.asJava).asJava.toCompletableFuture

  override def listByPrefix(prefix: String): CompletableFuture[util.List[Location]] =
    locationService.listByPrefix(prefix).map(_.asJava).asJava.toCompletableFuture

  override def track(connection: Connection): Source[TrackingEvent, Subscription] =
    locationService.track(connection).asJava

  override def subscribe(connection: Connection, consumer: Consumer[TrackingEvent]): Subscription =
    locationService.subscribe(connection, consumer.accept)

  override def asScala: LocationService = locationService

  private def registrationResult(registrationResult: RegistrationResult): IRegistrationResult =
    new IRegistrationResult {
      override def unregister: CompletableFuture[Done] = registrationResult.unregister().asJava.toCompletableFuture

      override def location: Location = registrationResult.location
    }
}
