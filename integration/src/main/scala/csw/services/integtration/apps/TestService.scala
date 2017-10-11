package csw.services.integtration.apps

import csw.messages.location.Connection.HttpConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.integtration.common.RegistrationFactory
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.LocationServiceFactory

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
