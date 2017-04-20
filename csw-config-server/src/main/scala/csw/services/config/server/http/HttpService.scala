package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.Http
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future

class HttpService(locationService: LocationService,
                  configServiceRoute: ConfigServiceRoute,
                  settings: Settings,
                  actorRuntime: ActorRuntime) {

  import actorRuntime._

  lazy val lazyBinding: Future[ConfigServiceBinding] = async {
    val binding = await(
      Http().bindAndHandle(
        handler = configServiceRoute.route,
        interface = ClusterAwareSettings.hostname,
        port = settings.`service-port`
      )
    )
    println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    println("==== Registering Config Service HTTP Server with Location Service ====")
    ConfigServiceBinding(binding, await(register()))
  }

  def shutdown(): Future[Done] = async {
    val configServiceBinding = await(lazyBinding)
    await(configServiceBinding.registrationResult.unregister())
    await(configServiceBinding.serverBinding.unbind())
    await(actorSystem.terminate())
    await(locationService.shutdown())
    Done
  }

  private def register(): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service)),
      port = settings.`service-port`,
      path = ""
    )
    locationService.register(registration)
  }

}
