package csw.location.server.http

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import csw.location.server.internal.{ActorRuntime, Settings}
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: LocationRoutes, actorRuntime: ActorRuntime, settings: Settings) {

  import actorRuntime._

  def resourceStream(resourceName: String): InputStream = {
    val is = getClass.getClassLoader.getResourceAsStream(resourceName)
    require(is ne null, s"Resource $resourceName not found")
    is
  }

  val serverContext: HttpsConnectionContext = {
    // never put passwords into code!
    val password = "abcdef".toCharArray

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(resourceStream("keys/server.p12"), password)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    new HttpsConnectionContext(context)
  }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandleAsync(
      handler = Route.asyncHandler(locationRoutes.routes),
      interface = "0.0.0.0",
      port = settings.httpPort,
      connectionContext = serverContext
    )
  }
}
