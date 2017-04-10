package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.Http
import csw.services.config.api.commons.ActorRuntime
import csw.services.config.server.Settings

import scala.async.Async._
import scala.concurrent.Future

class HttpService(configServiceRoute: ConfigServiceRoute, settings: Settings, actorRuntime: ActorRuntime) {

  import actorRuntime._

  lazy val lazyBinding: Future[Http.ServerBinding] = Http().bindAndHandle(
    handler = configServiceRoute.route,
    interface = "localhost",
    port = settings.`service-port`
  ).map { binding =>
    println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    binding
  }

  def shutdown(): Future[Done] = async {
    await(await(lazyBinding).unbind())
    await(actorRuntime.actorSystem.terminate())
    Done
  }

}
