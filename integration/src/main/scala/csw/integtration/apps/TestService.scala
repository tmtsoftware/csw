package csw.integtration.apps

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.integtration.common.RegistrationFactory
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.scaladsl.LocationServiceFactory

object TestService {
  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection  = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private val Port = 9999

  val registration = RegistrationFactory.http(connection, Port, Path)
  val registrationResult =
    LocationServiceFactory.make().register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {}
}
