package csw.services.integtration.crdt

import java.net.URI

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.internal.LocationServiceCrdtImpl
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, HttpRegistration, ResolvedHttpLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TestServiceCrdt {
  private val actorRuntime = new ActorRuntime("crdt", 2553)

  val connection = HttpConnection(ComponentId("redisservice", ComponentType.Service))

  private val Path = "redisservice.org/test"
  private val uri = new URI(s"http://${actorRuntime.hostname}:9999/$Path")
  val registration = ResolvedHttpLocation(connection, uri, Path)

  private val locationService = new LocationServiceCrdtImpl(actorRuntime)

  val registrationResult = locationService.register(registration).await

  print("Redis Service Registered")

  def main(args: Array[String]): Unit = {

  }
}
