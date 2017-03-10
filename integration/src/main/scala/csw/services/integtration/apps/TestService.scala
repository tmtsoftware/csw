package csw.services.integtration.apps

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.common.ActorRuntime
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, HttpRegistration}
import csw.services.location.scaladsl.LocationServiceFactory

object TestService extends App{
  private val actorRuntime = new ActorRuntime("test-service")

  val componentId = ComponentId("testservice", ComponentType.Service)
  val connection = HttpConnection(componentId)

  val registration = HttpRegistration(connection, port=9999, "testService.org/test")
  val registrationResult = LocationServiceFactory.make(actorRuntime).register(registration).await

  print("Test Service Started")
}
