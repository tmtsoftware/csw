package csw.auth.installed

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.aas.installed.InstalledAppAuthAdapterFactory
import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.aas.installed.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.ExecutionContextExecutor

// #adapter-factory
object AdapterFactory {
  def makeAdapter(implicit actorSystem: ActorSystem): InstalledAppAuthAdapter = {
    implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
    implicit val mat: Materializer            = ActorMaterializer()
    val locationService: LocationService      = HttpLocationServiceFactory.makeLocalClient(actorSystem, mat)
    val authStore                             = new FileAuthStore(Paths.get("/tmp/demo-cli/auth"))
    InstalledAppAuthAdapterFactory.make(locationService, authStore)
  }
}
// #adapter-factory
