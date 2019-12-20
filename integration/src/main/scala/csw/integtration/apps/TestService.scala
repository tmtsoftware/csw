package csw.integtration.apps

import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.scaladsl.RegistrationResult
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.impl.internal.ServerWiring
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType, HttpRegistration}
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.{Prefix, Subsystem}

object TestService {
  val componentId = ComponentId(Prefix(Subsystem.CSW, "redisservice"), ComponentType.Service)
  val connection  = HttpConnection(componentId)

  private val Path = "redisservice.org/test"
  private val Port = 9999

  val adminWiring: ServerWiring = ServerWiring.make(Some(3553), "csw-location-server")
  LoggingSystemFactory.start("Assembly", "1.0", adminWiring.clusterSettings.hostname, adminWiring.actorSystem)

  adminWiring.locationHttpService.start().await

  import adminWiring.actorRuntime._

  val registration                           = HttpRegistration(connection, Port, Path)
  val registrationResult: RegistrationResult = HttpLocationServiceFactory.makeLocalClient.register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {}
}
