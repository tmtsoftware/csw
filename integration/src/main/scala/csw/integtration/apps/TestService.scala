package csw.integtration.apps

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.clusterseed.client.HTTPLocationService
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration, RegistrationResult}
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.client.scaladsl.HttpLocationServiceFactory

object TestService extends HTTPLocationService {
  val componentId = ComponentId("redisservice", ComponentType.Service)
  val connection  = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private val Port = 9999

  val registration                            = HttpRegistration(connection, Port, Path)
  implicit private val system: ActorSystem    = ActorSystem()
  implicit private val mat: ActorMaterializer = ActorMaterializer()
  val registrationResult: RegistrationResult  = HttpLocationServiceFactory.makeLocalClient.register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {}
}
