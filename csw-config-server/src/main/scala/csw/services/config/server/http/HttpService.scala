package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

class HttpService(locationService: LocationService,
                  configServiceRoute: ConfigServiceRoute,
                  settings: Settings,
                  actorRuntime: ActorRuntime) {

  import actorRuntime._

  lazy val registeredLazyBinding: Future[(ServerBinding, RegistrationResult)] = async {
    val binding            = await(bind())
    val registrationResult = await(register(binding))
    println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    (binding, registrationResult)
  }

  def shutdown(): Future[Done] = async {
    val (binding, registrationResult) = await(registeredLazyBinding)
    await(registrationResult.unregister())
    await(binding.unbind())
    await(locationService.shutdown())
    await(actorSystem.terminate())
    Done
  }

  private def bind() =
    Http().bindAndHandle(
      handler = configServiceRoute.route,
      interface = ClusterAwareSettings.hostname,
      port = settings.`service-port`
    ) recoverWith {
      case NonFatal(ex) ⇒
        async {
          await(locationService.shutdown())
          await(actorSystem.terminate())
          throw ex
        }
    }

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service)),
      port = binding.localAddress.getPort,
      path = ""
    )
    println("==== Registering Config Service HTTP Server with Location Service ====")
    locationService.register(registration)
  } recoverWith {
    case NonFatal(ex) ⇒
      async {
        await(binding.unbind())
        await(locationService.shutdown())
        await(actorSystem.terminate())
        throw ex
      }
  }
}
