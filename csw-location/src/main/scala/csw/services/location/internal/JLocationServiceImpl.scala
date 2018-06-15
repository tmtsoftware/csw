package csw.services.location.internal

import java.util
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.messages.location._
import csw.services.location.javadsl.{ILocationService, IRegistrationResult}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

private[location] class JLocationServiceImpl(locationService: LocationService)(implicit ec: ExecutionContext)
    extends ILocationService {

  override def register(registration: Registration): CompletableFuture[IRegistrationResult] =
    locationService.register(registration).map(registrationResult).toJava.toCompletableFuture

  override def unregister(connection: Connection): CompletableFuture[Done] =
    locationService.unregister(connection).toJava.toCompletableFuture

  override def unregisterAll(): CompletableFuture[Done] =
    locationService.unregisterAll().toJava.toCompletableFuture

  override def find[L <: Location](connection: TypedConnection[L]): CompletableFuture[Optional[L]] =
    locationService.find(connection).map(_.asJava).toJava.toCompletableFuture

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): CompletableFuture[Optional[L]] =
    locationService.resolve(connection, within).map(_.asJava).toJava.toCompletableFuture

  override def list: CompletableFuture[util.List[Location]] =
    locationService.list.map(_.asJava).toJava.toCompletableFuture

  override def list(componentType: ComponentType): CompletableFuture[util.List[Location]] =
    locationService.list(componentType).map(_.asJava).toJava.toCompletableFuture

  override def list(hostname: String): CompletableFuture[util.List[Location]] =
    locationService.list(hostname).map(_.asJava).toJava.toCompletableFuture

  override def list(connectionType: ConnectionType): CompletableFuture[util.List[Location]] =
    locationService.list(connectionType).map(_.asJava).toJava.toCompletableFuture

  override def listByPrefix(prefix: String): CompletableFuture[util.List[AkkaLocation]] =
    locationService.listByPrefix(prefix).map(_.asJava).toJava.toCompletableFuture

  override def track(connection: Connection): Source[TrackingEvent, KillSwitch] =
    locationService.track(connection).asJava

  override def subscribe(connection: Connection, consumer: Consumer[TrackingEvent]): KillSwitch =
    locationService.subscribe(connection, consumer.accept)

  override def shutdown(reason: Reason): CompletableFuture[Done] = locationService.shutdown(reason).toJava.toCompletableFuture

  override def asScala: LocationService = locationService

  private def registrationResult(registrationResult: RegistrationResult): IRegistrationResult =
    new IRegistrationResult {
      override def unregister: CompletableFuture[Done] = registrationResult.unregister().toJava.toCompletableFuture

      override def location: Location = registrationResult.location
    }
}
