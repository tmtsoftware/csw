package csw.services.location.javadsl
import java.util
import java.util.concurrent.CompletableFuture

import akka.Done
import csw.services.location.common.ActorRuntime
import csw.services.location.models.{Location, Registration, RegistrationResult}
import csw.services.location.scaladsl.LocationServiceFactory

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._

class JLocationServiceImpl(actorRuntime: ActorRuntime) extends ILocationService{
  import actorRuntime.actorSystem.dispatcher
  val locationService = LocationServiceFactory.make(actorRuntime)

  override def register(registration: Registration): CompletableFuture[RegistrationResult] = locationService.register(registration).toJava.toCompletableFuture

  override def list(): CompletableFuture[util.List[Location]] =  locationService.list.map(_.asJava).toJava.toCompletableFuture

  override def unregisterAll(): CompletableFuture[Done] = locationService.unregisterAll().toJava.toCompletableFuture;
}
