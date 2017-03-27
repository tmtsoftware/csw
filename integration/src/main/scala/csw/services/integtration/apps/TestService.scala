package csw.services.integtration.apps

import java.net.URI

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.{ComponentId, ComponentType, HttpLocation}
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TestService {
  private val actorRuntime = new ActorRuntime()

  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private  val Port = 9999

  val registration = new HttpLocation(connection, actorRuntime.hostname, Port, Path)
  val registrationResult =
    LocationServiceFactory.make(actorRuntime).register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {

  }
}
