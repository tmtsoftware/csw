package csw.services.integtration.apps

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.models.Connection.HttpConnection
import csw.services.location.scaladsl.models.{ComponentId, ComponentType, HttpRegistration}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TestService extends App{
  private val actorRuntime = new ActorRuntime("test-service")

  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection = HttpConnection(componentId)

  val registration = HttpRegistration(connection, port=9999, "redisservice.org/test")
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  print("Redis Service Registered")
}
