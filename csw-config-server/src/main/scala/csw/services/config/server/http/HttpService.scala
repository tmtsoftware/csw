package csw.services.config.server.http

import akka.Done
import akka.actor.CoordinatedShutdown
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

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering ${registrationResult.location}"
    )(() => registrationResult.unregister())

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseServiceUnbind,
      s"unregistering ${registrationResult.location}"
    )(() ⇒ locationService.shutdown())

    println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    (binding, registrationResult)
  } recoverWith {
    case NonFatal(ex) ⇒
      async {
        await(shutdown())
        throw ex
      }
  }

  def shutdown(): Future[Done] = actorRuntime.shutdown()

  private def bind() = Http().bindAndHandle(
    handler = configServiceRoute.route,
    interface = ClusterAwareSettings.hostname,
    port = settings.`service-port`
  )

  private def register(binding: ServerBinding): Future[RegistrationResult] = {
    val registration = HttpRegistration(
      connection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service)),
      port = binding.localAddress.getPort,
      path = ""
    )
    println("==== Registering Config Service HTTP Server with Location Service ====")
    locationService.register(registration)
  }
}
