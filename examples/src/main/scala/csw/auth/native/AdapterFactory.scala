package csw.auth.native

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.aas.native.NativeAppAuthAdapterFactory
import csw.aas.native.api.NativeAppAuthAdapter
import csw.aas.native.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.ExecutionContextExecutor

// #adapter-factory
object AdapterFactory {
  def makeAdapter(implicit actorSystem: ActorSystem): NativeAppAuthAdapter = {
    implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
    implicit val mat: Materializer            = ActorMaterializer()
    val locationService: LocationService      = HttpLocationServiceFactory.makeLocalClient(actorSystem, mat)
    val authStore                             = new FileAuthStore(Paths.get("/tmp/demo-cli/auth"))
    NativeAppAuthAdapterFactory.make(locationService, authStore)
  }
}
// #adapter-factory
