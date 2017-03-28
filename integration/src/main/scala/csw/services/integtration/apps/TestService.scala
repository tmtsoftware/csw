package csw.services.integtration.apps

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, HttpRegistration}
import csw.services.location.scaladsl.LocationServiceFactory

object TestService {
  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private  val Port = 9999

  val registration = HttpRegistration(connection,  Port, Path)
  val registrationResult =
    LocationServiceFactory.make().register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {

  }
}
