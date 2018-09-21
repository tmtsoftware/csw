package csw.integtration.apps

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration}
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.scaladsl.LocationServiceFactory

object TestService {
  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection  = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private val Port = 9999

  val registration = HttpRegistration(connection, Port, Path)
  val registrationResult =
    LocationServiceFactory.make().register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {}
}
